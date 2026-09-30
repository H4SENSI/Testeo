package com.h4sensi.street11;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameView extends FrameLayout {
    private final GLSurfaceView glView;
    private final FootballRenderer renderer;
    private final Hud hud;

    public GameView(Context context) {
        super(context);
        setWillNotDraw(false);

        glView = new GLSurfaceView(context);
        glView.setEGLContextClientVersion(2);
        renderer = new FootballRenderer();
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        addView(glView, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        hud = new Hud(context);
        addView(hud, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private void runOnGl(Runnable action) {
        glView.queueEvent(action);
    }

    private class Hud extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float joyX, joyY;
        private boolean joyTouch;
        private boolean matchStarted;

        Hud(Context context) {
            super(context);
            t.setTypeface(Typeface.create("sans", Typeface.BOLD));
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            int w = getWidth(), h = getHeight();
            FootballRenderer.Snapshot s = renderer.snapshot();

            if (!matchStarted) {
                drawMenu(c, w, h);
            } else {
                drawHud(c, w, h, s);
            }

            postInvalidateOnAnimation();
        }

        private void drawMenu(Canvas c, int w, int h) {
            p.setColor(Color.argb(145, 3, 8, 13));
            c.drawRect(0, 0, w, h, p);

            p.setColor(Color.argb(235, 8, 17, 26));
            c.drawRoundRect(new RectF(w * .08f, h * .12f, w * .92f, h * .88f), 30, 30, p);

            text(c, "STREET 11", w * .5f, h * .29f, 58, Color.WHITE, true);
            text(c, "3D MOBILE FOOTBALL", w * .5f, h * .36f, 16, Color.rgb(103, 220, 177), true);
            text(c, "QUICK MATCH", w * .5f, h * .42f, 13, Color.rgb(147, 160, 175), true);

            p.setColor(Color.rgb(47, 161, 255));
            c.drawRoundRect(new RectF(w * .27f, h * .53f, w * .73f, h * .66f), 18, 18, p);
            text(c, "PLAY", w * .5f, h * .615f, 20, Color.WHITE, true);

            text(c, "3D CAMERA  •  22 PLAYERS  •  OFFLINE", w * .5f, h * .75f, 11,
                    Color.rgb(114, 133, 151), true);
            text(c, "H4SENSI FC", w * .5f, h * .80f, 11,
                    Color.rgb(99, 185, 255), true);
        }

        private void drawHud(Canvas c, int w, int h, FootballRenderer.Snapshot s) {
            p.setColor(Color.argb(220, 5, 11, 17));
            c.drawRoundRect(new RectF(w * .31f, 15, w * .69f, 75), 18, 18, p);

            text(c, "BLUE UNITED", w * .37f, 38, 10, Color.rgb(133, 192, 255), true);
            text(c, "RED ATHLETIC", w * .63f, 38, 10, Color.rgb(255, 143, 156), true);
            text(c, s.home + "  -  " + s.away, w * .5f, 58, 25, Color.WHITE, true);
            text(c, String.format(Locale.US, "%02d:%02d", (int)(s.clock / 60), (int)s.clock % 60),
                    w * .5f, 76, 10, Color.rgb(170, 182, 193), true);

            p.setColor(Color.argb(160, 9, 16, 23));
            c.drawRoundRect(new RectF(w - 68, 18, w - 20, 62), 12, 12, p);
            text(c, "Ⅱ", w - 44, 47, 18, Color.WHITE, true);

            float bx = 105, by = h - 112;
            p.setColor(Color.argb(65, 255,255,255));
            c.drawCircle(bx, by, 70, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            p.setColor(Color.argb(90,255,255,255));
            c.drawCircle(bx, by, 70, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(205,255,255,255));
            c.drawCircle(bx + joyX * 38, by + joyY * 38, 27, p);

            action(c, w - 96, h - 105, 54, "SHOT", Color.rgb(246, 67, 91));
            action(c, w - 210, h - 158, 44, "PASS", Color.rgb(50, 155, 255));

            text(c, "MOVE", 73, h - 28, 10, Color.argb(180,255,255,255), true);

            if (s.state == FootballRenderer.PAUSED || s.state == FootballRenderer.RESULT) {
                p.setColor(Color.argb(165, 0, 0, 0));
                c.drawRect(0, 0, w, h, p);
            }

            if (s.state == FootballRenderer.PAUSED) {
                panel(c, w, h);
                text(c, "MATCH PAUSED", w*.5f, h*.34f, 28, Color.WHITE, true);
                button(c, w*.5f, h*.52f, "RESUME");
                button(c, w*.5f, h*.64f, "QUIT");
            } else if (s.state == FootballRenderer.RESULT) {
                panel(c, w, h);
                String title = s.home > s.away ? "VICTORY" : (s.home < s.away ? "DEFEAT" : "DRAW");
                text(c, title, w*.5f, h*.34f, 40,
                        title.equals("VICTORY") ? Color.rgb(96, 227, 165) : Color.WHITE, true);
                text(c, s.home + "   -   " + s.away, w*.5f, h*.48f, 46, Color.WHITE, true);
                button(c, w*.5f, h*.66f, "PLAY AGAIN");
            }
        }

        private void panel(Canvas c, int w, int h) {
            p.setColor(Color.rgb(11, 19, 28));
            c.drawRoundRect(new RectF(w*.30f, h*.20f, w*.70f, h*.80f), 26, 26, p);
        }

        private void button(Canvas c, float x, float y, String label) {
            p.setColor(label.equals("QUIT") ? Color.rgb(47, 59, 73) : Color.rgb(48, 160, 255));
            c.drawRoundRect(new RectF(x-120, y-26, x+120, y+26), 13, 13, p);
            text(c, label, x, y+6, 13, Color.WHITE, true);
        }

        private void action(Canvas c, float x, float y, float radius, String label, int color) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(80,0,0,0));
            c.drawCircle(x+3, y+5, radius+2, p);
            p.setColor(color);
            c.drawCircle(x, y, radius, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(Color.argb(160,255,255,255));
            c.drawCircle(x, y, radius, p);
            p.setStyle(Paint.Style.FILL);
            text(c, label, x, y+4, label.equals("PASS") ? 11 : 12, Color.WHITE, true);
        }

        private void text(Canvas c, String s, float x, float y, float size, int color, boolean center) {
            t.setTextSize(size);
            t.setColor(color);
            t.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT);
            c.drawText(s, x, y, t);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            int w = getWidth(), h = getHeight();

            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                FootballRenderer.Snapshot s = renderer.snapshot();

                if (!matchStarted) {
                    if (x > w*.25f && x < w*.75f && y > h*.50f && y < h*.70f) {
                        matchStarted = true;
                        runOnGl(() -> renderer.startMatch());
                        invalidate();
                        performClick();
                    }
                    return true;
                }

                if (s.state == FootballRenderer.MATCH) {
                    if (x > w - 90 && y < 90) {
                        runOnGl(() -> renderer.pause());
                        performClick();
                        return true;
                    }
                    if (distance(x,y,w-96,h-105) < 72) {
                        runOnGl(() -> renderer.shoot());
                        performClick();
                        return true;
                    }
                    if (distance(x,y,w-210,h-158) < 60) {
                        runOnGl(() -> renderer.pass());
                        performClick();
                        return true;
                    }
                    if (x < w*.45f && y > h*.50f) {
                        joyTouch = true;
                        updateJoy(x,y,w,h);
                        return true;
                    }
                } else if (s.state == FootballRenderer.PAUSED) {
                    if (Math.abs(x-w*.5f)<150 && Math.abs(y-h*.52f)<38) {
                        runOnGl(() -> renderer.resume());
                        performClick();
                        return true;
                    }
                    if (Math.abs(x-w*.5f)<150 && Math.abs(y-h*.64f)<38) {
                        matchStarted = false;
                        runOnGl(() -> renderer.backToMenu());
                        performClick();
                        invalidate();
                        return true;
                    }
                } else if (s.state == FootballRenderer.RESULT) {
                    if (Math.abs(x-w*.5f)<160 && Math.abs(y-h*.66f)<42) {
                        runOnGl(() -> renderer.startMatch());
                        performClick();
                        return true;
                    }
                }
            } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE && joyTouch) {
                updateJoy(x,y,w,h);
                return true;
            } else if (e.getActionMasked() == MotionEvent.ACTION_UP
                    || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                joyTouch = false;
                joyX = 0; joyY = 0;
                runOnGl(() -> renderer.setJoystick(0,0));
                return true;
            }
            return true;
        }

        private void updateJoy(float x,float y,int w,int h) {
            float dx=x-105, dy=y-(h-112);
            float d=(float)Math.sqrt(dx*dx+dy*dy);
            if(d>70){dx=dx/d*70;dy=dy/d*70;}
            joyX=dx/70f; joyY=dy/70f;
            runOnGl(() -> renderer.setJoystick(joyX,joyY));
            invalidate();
        }

        private float distance(float a,float b,float c,float d) {
            float x=a-c,y=b-d; return (float)Math.sqrt(x*x+y*y);
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
    }

    static class FootballRenderer implements GLSurfaceView.Renderer {
        static final int MENU=0, MATCH=1, PAUSED=2, RESULT=3;

        private final float[] proj=new float[16];
        private final float[] view=new float[16];
        private final float[] vp=new float[16];
        private final float[] model=new float[16];
        private final float[] mvp=new float[16];
        private final float[] lightM=new float[16];
        private int program;
        private int uMvp,uModel,uColor;
        private Mesh cube,sphere,cylinder,plane;
        private int width,height;
        private volatile int state=MENU;
        private volatile float joyX,joyY;
        private float clock;
        private int homeScore,awayScore;
        private Ball ball=new Ball();
        private final List<Player> players=new ArrayList<>();

        private static final float FIELD_X=10.6f;
        private static final float FIELD_Z=17.6f;

        @Override public void onSurfaceCreated(GL10 gl,EGLConfig config) {
            GLES20.glClearColor(0.02f,0.05f,0.08f,1);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            program=Shader.build();
            uMvp=GLES20.glGetUniformLocation(program,"uMVP");
            uModel=GLES20.glGetUniformLocation(program,"uModel");
            uColor=GLES20.glGetUniformLocation(program,"uColor");
            cube=Mesh.cube();
            sphere=Mesh.sphere(12,10);
            cylinder=Mesh.cylinder(12);
            plane=Mesh.plane();
        }

        @Override public void onSurfaceChanged(GL10 gl,int w,int h) {
            width=w; height=h;
            GLES20.glViewport(0,0,w,h);
            float aspect=(float)w/Math.max(1,h);
            Matrix.perspectiveM(proj,0,48f,aspect,0.1f,100f);
            camera();
        }

        @Override public void onDrawFrame(GL10 gl) {
            long now=System.nanoTime();
            if(lastNs==0) lastNs=now;
            float dt=Math.min(0.033f,(now-lastNs)/1_000_000_000f);
            lastNs=now;

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            camera();

            if(state==MATCH) update(dt);
            drawStadium();
            drawField();
            drawPlayers();
            drawBall();

            if(state==RESULT){
                // Keep the world frozen behind the HUD.
            }
        }
        private long lastNs;

        private void camera() {
            float camY=11.7f, camZ=14.5f;
            Matrix.setLookAtM(view,0,0,camY,camZ,0,0,0,0,1,0);
            Matrix.multiplyMM(vp,0,proj,0,view,0);
        }

        void startMatch() {
            state=MATCH; clock=0; homeScore=0; awayScore=0; buildTeams();
        }
        void pause(){ if(state==MATCH) state=PAUSED; }
        void resume(){ if(state==PAUSED) state=MATCH; }
        void backToMenu(){ state=MENU; }
        void setJoystick(float x,float y){joyX=x; joyY=y;}

        Snapshot snapshot(){ return new Snapshot(state,clock,homeScore,awayScore); }

        private void buildTeams() {
            players.clear();
            float[][] home={{-4.55f,0.0f},{-2.9f,-5.7f},{-2.9f,5.7f},{-1.0f,-3.2f},{-1.0f,3.2f},
                    {1.0f,-5.3f},{0.2f,-1.7f},{0.2f,1.7f},{2.9f,-4.0f},{2.9f,4.0f},{4.15f,0.0f}};
            for(int i=0;i<home.length;i++) players.add(new Player(home[i][0],home[i][1],true,i+1));
            for(int i=0;i<home.length;i++) players.add(new Player(-home[i][0],home[i][1],false,i+1));
            ball.x=0; ball.z=0; ball.vx=0; ball.vz=0;
        }

        private void update(float dt) {
            clock+=dt;
            if(clock>=90f){clock=90f;state=RESULT;return;}

            Player user=players.get(6);
            float speed=5.2f;
            user.vx=joyX*speed;
            user.vz=joyY*speed;
            user.x+=user.vx*dt;
            user.z+=user.vz*dt;

            for(Player p:players){
                if(p!=user){
                    float tx=p.baseX, tz=p.baseZ;
                    float dist=(float)Math.hypot(ball.x-p.x,ball.z-p.z);
                    if(!p.home && dist<5.5f){tx=ball.x;tz=ball.z;}
                    else if(p.home && dist<3.2f && p.number!=1){tx=ball.x-0.7f;tz=ball.z;}
                    float dx=tx-p.x, dz=tz-p.z, d=(float)Math.hypot(dx,dz);
                    if(d>0.12f){
                        float sp=p.home?2.3f:2.8f;
                        p.vx=dx/d*sp; p.vz=dz/d*sp;
                        p.x+=p.vx*dt; p.z+=p.vz*dt;
                    }
                }
                p.x=clamp(p.x,-FIELD_X/2+0.5f,FIELD_X/2-0.5f);
                p.z=clamp(p.z,-FIELD_Z/2+0.5f,FIELD_Z/2-0.5f);
            }

            float d=(float)Math.hypot(ball.x-user.x,ball.z-user.z);
            if(d<1.25f){
                ball.vx+=joyX*7.5f*dt;
                ball.vz+=joyY*7.5f*dt;
                if(Math.hypot(joyX,joyY)>0.15) {
                    ball.ownerHome=true;
                }
            }

            ball.x+=ball.vx*dt; ball.z+=ball.vz*dt;
            float fr=(float)Math.pow(0.985,dt*60f);
            ball.vx*=fr; ball.vz*=fr;

            float goalHalf=1.65f;
            if(ball.z>FIELD_Z/2+0.7f){
                if(Math.abs(ball.x)<goalHalf){homeScore++;resetBall();}
                else {ball.z=FIELD_Z/2-0.1f;ball.vz=-Math.abs(ball.vz)*0.7f;}
            }
            if(ball.z<-FIELD_Z/2-0.7f){
                if(Math.abs(ball.x)<goalHalf){awayScore++;resetBall();}
                else {ball.z=-FIELD_Z/2+0.1f;ball.vz=Math.abs(ball.vz)*0.7f;}
            }
            if(ball.x<-FIELD_X/2+0.15f){ball.x=-FIELD_X/2+0.15f;ball.vx=Math.abs(ball.vx)*0.7f;}
            if(ball.x>FIELD_X/2-0.15f){ball.x=FIELD_X/2-0.15f;ball.vx=-Math.abs(ball.vx)*0.7f;}
        }

        private void resetBall(){
            ball.x=0;ball.z=0;ball.vx=0;ball.vz=0;
            for(Player p:players){p.x=p.baseX;p.z=p.baseZ;p.vx=0;p.vz=0;}
        }

        void shoot(){
            if(state!=MATCH)return;
            Player user=players.get(6);
            if(Math.hypot(ball.x-user.x,ball.z-user.z)<1.65f){
                float aimZ=FIELD_Z/2+1.2f;
                float dz=aimZ-ball.z, dx=-ball.x*0.22f;
                float d=(float)Math.hypot(dx,dz);
                ball.vx=dx/d*18f; ball.vz=dz/d*18f; ball.ownerHome=true;
            }
        }

        void pass(){
            if(state!=MATCH)return;
            Player user=players.get(6), target=null;
            float best=999;
            for(Player p:players) if(p.home && p!=user){
                float d=(float)Math.hypot(p.x-user.x,p.z-user.z);
                if(d<best && p.number!=1 && p.x>user.x-1){best=d;target=p;}
            }
            if(target!=null && Math.hypot(ball.x-user.x,ball.z-user.z)<1.7f){
                float dx=target.x-ball.x,dz=target.z-ball.z,d=(float)Math.hypot(dx,dz);
                ball.vx=dx/d*11f;ball.vz=dz/d*11f;ball.ownerHome=true;
            }
        }

        private void drawStadium(){
            drawBox(0,-0.9f,FIELD_Z/2+2.8f,FIELD_X+4,1.8f,3.8f,new float[]{0.05f,0.09f,0.13f,1});
            drawBox(0,-0.9f,-FIELD_Z/2-2.8f,FIELD_X+4,1.8f,3.8f,new float[]{0.05f,0.09f,0.13f,1});
            drawBox(-FIELD_X/2-2.2f,-0.9f,0,3.8f,1.8f,FIELD_Z+5,new float[]{0.045f,0.08f,0.12f,1});
            drawBox(FIELD_X/2+2.2f,-0.9f,0,3.8f,1.8f,FIELD_Z+5,new float[]{0.045f,0.08f,0.12f,1});
        }

        private void drawField(){
            drawPlane(0,-0.16f,0,FIELD_X,FIELD_Z,new float[]{0.055f,0.42f,0.22f,1});
            for(int i=-8;i<=8;i+=2){
                drawPlane(-FIELD_X/2+0.001f,-0.15f,i,FIELD_X-0.002f,0.9f,
                        new float[]{0.06f+(i%4==0?0.018f:0),0.46f+(i%4==0?0.03f:0),0.24f,1});
            }
            float white=0.9f;
            drawThinBox(0,-0.04f,0,0.035f,0.02f,FIELD_Z,new float[]{white,white,white,1});
            drawArcCircle(0,-0.035f,0,2.9f,new float[]{1,1,1,1});
            drawLine(-FIELD_X/2+0.06f,-0.04f,0,FIELD_X*.18f,.02f,new float[]{1,1,1,1});
            drawLine(FIELD_X/2-0.06f,-0.04f,0,FIELD_X*.18f,.02f,new float[]{1,1,1,1});
            drawGoal(0,0,FIELD_Z/2);
            drawGoal(0,0,-FIELD_Z/2);
        }

        private void drawPlayers(){
            for(Player p:players){
                float col=p.home?0.05f:0.86f;
                float[] jersey=p.home?new float[]{0.08f,0.42f,0.95f,1}:new float[]{0.92f,0.08f,0.16f,1};
                float[] shorts=p.home?new float[]{0.04f,0.11f,0.25f,1}:new float[]{0.22f,0.02f,0.04f,1};
                drawCylinder(p.x,0.42f,p.z,0.25f,0.75f,jersey);
                drawCylinder(p.x-0.12f,0.02f,p.z,0.10f,0.55f,shorts);
                drawCylinder(p.x+0.12f,0.02f,p.z,0.10f,0.55f,shorts);
                drawSphere(p.x,1.18f,p.z,0.20f,new float[]{0.9f,0.73f,0.58f,1});
                drawBox(p.x-0.19f,-0.24f,p.z,0.16f,0.10f,0.30f,shorts);
                drawBox(p.x+0.19f,-0.24f,p.z,0.16f,0.10f,0.30f,shorts);
                if(p==players.get(6)) drawRing(p.x,1.55f,p.z,0.28f,new float[]{1,1,1,0.85f});
                // shoulder/number accent
                drawBox(p.x,0.48f,p.z-0.20f,0.30f,0.16f,0.03f,new float[]{0.97f,0.97f,0.97f,1});
            }
        }

        private void drawBall(){
            drawSphere(ball.x,0.15f,ball.z,0.15f,new float[]{0.98f,0.98f,0.98f,1});
            drawSphere(ball.x,0.15f,ball.z,0.152f,new float[]{0.12f,0.12f,0.12f,0.18f});
        }

        private void drawGoal(float x,float y,float z){
            float sign=z>0?1:-1;
            float[] white={0.95f,0.95f,0.95f,1};
            drawBox(x-1.65f,y+1.45f,z,0.09f,2.9f,0.09f,white);
            drawBox(x+1.65f,y+1.45f,z,0.09f,2.9f,0.09f,white);
            drawBox(x,y+2.9f,z,3.3f,0.09f,0.09f,white);
            drawBox(x,y+1.45f,z+sign*0.8f,3.25f,2.9f,0.07f,new float[]{0.9f,0.9f,0.9f,0.35f});
        }

        private void drawPlane(float x,float y,float z,float sx,float sz,float[] color){
            Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,sx,1,sz); drawMesh(plane,color);
        }
        private void drawBox(float x,float y,float z,float sx,float sy,float sz,float[] color){
            Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,sx,sy,sz); drawMesh(cube,color);
        }
        private void drawThinBox(float x,float y,float z,float sx,float sy,float sz,float[] color){
            drawBox(x,y,z,sx,sy,sz,color);
        }
        private void drawCylinder(float x,float y,float z,float radius,float height,float[] color){
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y+height/2,z);Matrix.scaleM(model,0,radius,height,radius);drawMesh(cylinder,color);
        }
        private void drawSphere(float x,float y,float z,float radius,float[] color){
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,radius,radius,radius);drawMesh(sphere,color);
        }
        private void drawRing(float x,float y,float z,float radius,float[] color){
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,radius,radius*0.25f,radius);drawMesh(sphere,color);
        }
        private void drawLine(float x,float y,float z,float length,float thick,float[] color){
            drawBox(x,y,z,0.02f,0.01f,length/2,color);
        }
        private void drawArcCircle(float x,float y,float z,float rad,float[] color){
            for(int i=0;i<40;i++){
                double a1=i*Math.PI*2/40;
                double a2=(i+1)*Math.PI*2/40;
                float x1=x+(float)Math.cos(a1)*rad,z1=z+(float)Math.sin(a1)*rad;
                float x2=x+(float)Math.cos(a2)*rad,z2=z+(float)Math.sin(a2)*rad;
                drawBox((x1+x2)/2,y,(z1+z2)/2,
                        0.018f,0.012f,(float)Math.hypot(x2-x1,z2-z1)/2,color);
            }
        }

        private void drawMesh(Mesh mesh,float[] color){
            GLES20.glUseProgram(program);
            Matrix.multiplyMM(mvp,0,vp,0,model,0);
            GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
            GLES20.glUniformMatrix4fv(uModel,1,false,model,0);
            GLES20.glUniform4fv(uColor,1,color,0);
            mesh.draw(program);
        }

        private float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}

        static class Snapshot {
            final int state; final float clock; final int home,away;
            Snapshot(int state,float clock,int home,int away){this.state=state;this.clock=clock;this.home=home;this.away=away;}
        }
        static class Player {
            float x,z,baseX,baseZ,vx,vz; final boolean home; final int number;
            Player(float x,float z,boolean home,int number){this.x=x;this.z=z;this.baseX=x;this.baseZ=z;this.home=home;this.number=number;}
        }
        static class Ball {
            float x,z,vx,vz; boolean ownerHome;
        }
    }

    static class Shader {
        static int build(){
            String vs="uniform mat4 uMVP; uniform mat4 uModel; attribute vec3 aPos; attribute vec3 aNormal; varying vec3 vN; void main(){gl_Position=uMVP*vec4(aPos,1.0); vN=mat3(uModel)*aNormal;}";
            String fs="precision mediump float; uniform vec4 uColor; varying vec3 vN; void main(){vec3 n=normalize(vN); vec3 l=normalize(vec3(0.35,1.0,0.5)); float d=max(dot(n,l),0.0); float light=0.38+d*0.62; gl_FragColor=vec4(uColor.rgb*light,uColor.a);}";
            int v=compile(GLES20.GL_VERTEX_SHADER,vs), f=compile(GLES20.GL_FRAGMENT_SHADER,fs);
            int pr=GLES20.glCreateProgram();
            GLES20.glAttachShader(pr,v);GLES20.glAttachShader(pr,f);GLES20.glBindAttribLocation(pr,0,"aPos");GLES20.glBindAttribLocation(pr,1,"aNormal");
            GLES20.glLinkProgram(pr);
            int[] ok=new int[1];GLES20.glGetProgramiv(pr,GLES20.GL_LINK_STATUS,ok,0);
            if(ok[0]==0){String log=GLES20.glGetProgramInfoLog(pr);GLES20.glDeleteProgram(pr);throw new RuntimeException(log);}
            return pr;
        }
        static int compile(int type,String src){
            int sh=GLES20.glCreateShader(type);GLES20.glShaderSource(sh,src);GLES20.glCompileShader(sh);
            int[] ok=new int[1];GLES20.glGetShaderiv(sh,GLES20.GL_COMPILE_STATUS,ok,0);
            if(ok[0]==0){String log=GLES20.glGetShaderInfoLog(sh);GLES20.glDeleteShader(sh);throw new RuntimeException(log);}
            return sh;
        }
    }

    static class Mesh {
        final FloatBuffer vertices,normals; final int count;
        Mesh(float[] v,float[] n){
            vertices=bb(v.length*4);vertices.put(v).position(0);
            normals=bb(n.length*4);normals.put(n).position(0);
            count=v.length/3;
        }
        void draw(int program){
            int ap=GLES20.glGetAttribLocation(program,"aPos"), an=GLES20.glGetAttribLocation(program,"aNormal");
            GLES20.glEnableVertexAttribArray(ap);GLES20.glEnableVertexAttribArray(an);
            vertices.position(0);normals.position(0);
            GLES20.glVertexAttribPointer(ap,3,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glVertexAttribPointer(an,3,GLES20.GL_FLOAT,false,0,normals);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            GLES20.glDisableVertexAttribArray(ap);GLES20.glDisableVertexAttribArray(an);
        }
        static FloatBuffer bb(int n){return ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder()).asFloatBuffer();}
        static Mesh plane(){
            float[] v={-0.5f,0,-0.5f, 0.5f,0,-0.5f, 0.5f,0,0.5f, -0.5f,0,-0.5f, 0.5f,0,0.5f, -0.5f,0,0.5f};
            float[] n=new float[18];for(int i=0;i<18;i+=3){n[i]=0;n[i+1]=1;n[i+2]=0;} return new Mesh(v,n);
        }
        static Mesh cube(){
            float[] v={
                -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f,
                0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f,
                -0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f, -0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f, -0.5f,0.5f,-0.5f,
                -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, -0.5f,-0.5f,0.5f,
                -0.5f,-0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f,
                0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f
            };
            float[] n=new float[108];
            for(int face=0;face<6;face++){float nx=0,ny=0,nz=0;if(face==0)nz=1;else if(face==1)nz=-1;else if(face==2)ny=1;else if(face==3)ny=-1;else if(face==4)nx=-1;else nx=1;for(int i=0;i<18;i+=3){int k=face*18+i;n[k]=nx;n[k+1]=ny;n[k+2]=nz;}}
            return new Mesh(v,n);
        }
        static Mesh cylinder(int seg){
            ArrayList<Float> vs=new ArrayList<>(), ns=new ArrayList<>();
            for(int i=0;i<seg;i++){
                double a1=i*Math.PI*2/seg,a2=(i+1)*Math.PI*2/seg;
                float x1=(float)Math.cos(a1),z1=(float)Math.sin(a1),x2=(float)Math.cos(a2),z2=(float)Math.sin(a2);
                tri(vs,ns,x1,-0.5f,z1,x2,-0.5f,z2,x2,0.5f,z2,nx(x1),0,nz(z1));
                tri(vs,ns,x1,-0.5f,z1,x2,0.5f,z2,x1,0.5f,z1,nx(x1),0,nz(z1));
                tri(vs,ns,0,0.5f,0,x1,0.5f,z1,x2,0.5f,z2,0,1,0);
                tri(vs,ns,0,-0.5f,0,x2,-0.5f,z2,x1,-0.5f,z1,0,-1,0);
            }
            return new Mesh(to(vs),to(ns));
        }
        static float nx(float x){return x;}
        static float nz(float z){return z;}
        static Mesh sphere(int seg,int rings){
            ArrayList<Float> vs=new ArrayList<>(),ns=new ArrayList<>();
            for(int y=0;y<rings;y++){
                double t1=Math.PI*y/rings-Math.PI/2,t2=Math.PI*(y+1)/rings-Math.PI/2;
                for(int x=0;x<seg;x++){
                    double a1=2*Math.PI*x/seg,a2=2*Math.PI*(x+1)/seg;
                    float[] p1=sp(a1,t1),p2=sp(a2,t1),p3=sp(a2,t2),p4=sp(a1,t2);
                    tri(vs,ns,p1,p2,p3);tri(vs,ns,p1,p3,p4);
                }
            }
            return new Mesh(to(vs),to(ns));
        }
        static float[] sp(double a,double t){float ct=(float)Math.cos(t);return new float[]{ct*(float)Math.cos(a),(float)Math.sin(t),ct*(float)Math.sin(a)};}
        static void tri(ArrayList<Float> v,ArrayList<Float> n,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float nx,float ny,float nz){
            add(v,ax,ay,az);add(v,bx,by,bz);add(v,cx,cy,cz);add(n,nx,ny,nz);add(n,nx,ny,nz);add(n,nx,ny,nz);
        }
        static void tri(ArrayList<Float> v,ArrayList<Float> n,float[] a,float[] b,float[] c){tri(v,n,a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2],a[0],a[1],a[2]);}
        static void add(ArrayList<Float> a,float x,float y,float z){a.add(x);a.add(y);a.add(z);}
        static float[] to(ArrayList<Float> a){float[] o=new float[a.size()];for(int i=0;i<o.length;i++)o[i]=a.get(i);return o;}
    }
}
