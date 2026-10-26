package de.pixelabenteuer.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.graphics.*;
import java.util.*;
import java.io.*;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);setContentView(new Game());}
 class Animal {
  String kind;float x,y,vx,vy;int hp;boolean dead=false;float t=0;int dir=0;boolean fleeing=false;
  Animal(String k,float a,float b){kind=k;x=a;y=b;hp=k.equals("boar")?4:2;}
 }
 class Game extends View {
  Canvas c;Paint p=new Paint();Random rng=new Random(42);ArrayList<Animal> animals=new ArrayList<>();
  HashMap<String,Bitmap> sprites=new HashMap<>();float px=200,py=200;int hp=5,coins=0;float joyX=0,joyY=0;boolean attacking=false;long prev=System.nanoTime();float elapsed=0;int attackCooldown=0;
  Game(){super(MainActivity.this);p.setFilterBitmap(false);setFocusable(true);for(int i=0;i<12;i++){String[] kinds={"boar","fox","deer","black_grouse"};animals.add(new Animal(kinds[i%4],100+rng.nextInt(1400),100+rng.nextInt(850)));}loadSprites();}
  void loadSprites(){String[] kinds={"boar","fox","deer","black_grouse"};String[] actions={"idle","walk","run","attack","flight","hurt","death"};String[] dirs={"front","back","left","right"};for(String k:kinds)for(String a:actions)for(String d:dirs){String key=k+"_"+a+"_"+d;try(InputStream in=getAssets().open("generated/"+key+".png")){sprites.put(key,BitmapFactory.decodeStream(in));}catch(Exception ignored){}}}
  void rect(int color,float x,float y,float w,float h){p.setColor(color);c.drawRect(x,y,x+w,y+h,p);}
  int color(String s){return Color.parseColor(s);}
  @Override protected void onDraw(Canvas canvas){c=canvas;float dt=Math.min(.05f,(System.nanoTime()-prev)/1e9f);prev=System.nanoTime();elapsed+=dt;float w=getWidth(),h=getHeight();float scale=Math.min(w/800f,h/450f);c.drawColor(color("#182b24"));c.save();c.scale(scale,scale);float vw=w/scale,vh=h/scale;float dx=vw/2-px,dy=vh/2-py;c.save();c.translate(dx,dy);
   for(int x=0;x<1600;x+=32)for(int y=0;y<1000;y+=32){int n=(x*37+y*19)%7;rect(n<3?color("#407d46"):color("#47884a"),x,y,32,32);if(n==1)rect(color("#6a9a53"),x+8,y+7,5,4);}
   for(int i=0;i<38;i++){float x=(i*131)%1550,y=(i*223)%950;rect(color("#315b35"),x,y,25,24);rect(color("#5b9c50"),x+4,y-9,18,24);}
   for(Animal a:animals){if(a.dead&&a.t>2)continue;float dist=(float)Math.hypot(px-a.x,py-a.y);a.t+=dt;String act="idle";if(!a.dead){if(a.kind.equals("boar")&&dist<210){a.vx=(px-a.x)/Math.max(1,dist)*65;a.vy=(py-a.y)/Math.max(1,dist)*65;act="run";if(dist<30&&a.t>1.1f){hp=Math.max(0,hp-1);a.t=0;act="attack";}}else if(dist<145){a.vx=(a.x-px)/Math.max(1,dist)*90;a.vy=(a.y-py)/Math.max(1,dist)*90;act=a.kind.equals("black_grouse")?"flight":"run";}else if(a.t>2){a.vx=(rng.nextFloat()-.5f)*40;a.vy=(rng.nextFloat()-.5f)*40;a.t=0;}else act="walk";a.x=Math.max(20,Math.min(1560,a.x+a.vx*dt));a.y=Math.max(20,Math.min(960,a.y+a.vy*dt));a.dir=Math.abs(a.vx)>Math.abs(a.vy)?(a.vx<0?2:3):(a.vy<0?1:0);}else act="death";drawSprite(a.kind,act,a.dir,a.x,a.y,48,48,a.t);if(!a.dead){p.setColor(Color.RED);c.drawRect(a.x-15,a.y-28,a.x-15+a.hp*8,a.y-25,p);}}
   rect(color("#203f86"),px-12,py-20,24,34);rect(color("#e5ba88"),px-9,py-28,18,14);rect(color("#f2dc71"),px-10,py-30,20,6);if(attacking){rect(Color.WHITE,px+12,py-8,22,5);attacking=false;}
   c.restore();rect(color("#142b24"),0,0,vw,35);p.setColor(Color.WHITE);p.setTextSize(19);c.drawText("PixelAbenteuer   ♥ "+hp+"   Tiere: "+animals.size()+"   WASD / Touch",12,24,p);
   rect(color("#263d40"),20,vh-108,90,90);rect(color("#496b67"),58,vh-100,14,74);rect(color("#496b67"),28,vh-69,74,14);rect(color("#a54c45"),vw-110,vh-102,88,88);p.setColor(Color.WHITE);p.setTextSize(22);c.drawText("HIT",vw-89,vh-50,p);
   if(hp==0){rect(color("#111111"),vw/2-160,vh/2-40,320,80);p.setColor(Color.WHITE);p.setTextSize(25);c.drawText("GAME OVER - Neustart tippen",vw/2-148,vh/2+8,p);}
   c.restore();if(hp>0){px=Math.max(12,Math.min(1580,px+joyX*dt*130));py=Math.max(12,Math.min(980,py+joyY*dt*130));}invalidate();}
  void drawSprite(String k,String action,int dir,float x,float y,float w,float h,float t){String d=new String[]{"front","back","left","right"}[dir];Bitmap b=sprites.get(k+"_"+action+"_"+d);if(b==null)b=sprites.get(k+"_idle_"+d);if(b==null){rect(k.equals("boar")?color("#8c5946"):k.equals("fox")?color("#d88035"):k.equals("deer")?color("#bfa173"):Color.BLACK,x-w/2,y-h/2,w,h);return;}int frames=Math.max(1,b.getWidth()/Math.max(1,b.getHeight()));int fw=b.getWidth()/frames;int frame=Math.min(frames-1,(int)(t*7)%frames);p.setColor(Color.WHITE);p.setFilterBitmap(false);c.drawBitmap(b,new Rect(frame*fw,0,(frame+1)*fw,b.getHeight()),new RectF(x-w/2,y-h/2,x+w/2,y+h/2),p);}
  void hit(){attacking=true;for(Animal a:animals){if(!a.dead&&Math.hypot(px-a.x,py-a.y)<72){a.hp--;a.t=0;if(a.hp<=0){a.dead=true;coins++;}}}}
  @Override public boolean onTouchEvent(android.view.MotionEvent e){float scale=Math.min(getWidth()/800f,getHeight()/450f);float x=e.getX()/scale,y=e.getY()/scale;float vh=getHeight()/scale,vw=getWidth()/scale;if(e.getAction()==1){joyX=joyY=0;return true;}if(hp==0){hp=5;px=200;py=200;for(Animal a:animals){a.dead=false;a.hp=2;}return true;}if(x>vw-135&&y>vh-135){if(e.getAction()==0)hit();}else if(x<140&&y>vh-140){joyX=Math.max(-1,Math.min(1,(x-65)/35));joyY=Math.max(-1,Math.min(1,(y-(vh-63))/35));}return true;}
  @Override public boolean onKeyDown(int key,android.view.KeyEvent e){switch(key){case 29:case 21:joyX=-1;break;case 32:case 22:joyX=1;break;case 51:case 19:joyY=-1;break;case 47:case 20:joyY=1;break;case 62:hit();break;}return true;}
  @Override public boolean onKeyUp(int key,android.view.KeyEvent e){joyX=joyY=0;return true;}
 }
}
