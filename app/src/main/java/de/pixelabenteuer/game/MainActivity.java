package de.pixelabenteuer.game;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
  setContentView(new Game());
 }
 class Mob {
  String kind,action="idle";float x,y,vx,vy,anim=0,decision=0,attackTimer=0,hurtTimer=0,deathTimer=0;int hp,maxHp,dir=0;boolean dead=false;
  Mob(String k,float xx,float yy) {kind=k;x=xx;y=yy;maxHp=k.equals("orc3")?7:k.equals("orc2")?5:k.equals("orc1")?4:k.equals("boar")?4:2;hp=maxHp;}
  boolean hostile(){return kind.startsWith("orc");}
 }
 class Decoration {
  Bitmap image;float x,y,w,h;boolean solid;
  Decoration(Bitmap b,float xx,float yy,float ww,float hh,boolean s){image=b;x=xx;y=yy;w=ww;h=hh;solid=s;}
 }
 class Game extends View {
  final int WORLD_W=1600,WORLD_H=1000;
  Canvas canvas;Paint paint=new Paint();Random random=new Random(42);
  ArrayList<Mob> mobs=new ArrayList<>();ArrayList<Decoration> decorations=new ArrayList<>();
  HashMap<String,Bitmap> sprites=new HashMap<>();
  float px=180,py=180,joyX=0,joyY=0,elapsed=0,attackAnim=0,hurtAnim=0,deathAnim=0,attackCooldown=0;
  int hp=8,maxHp=8,kills=0,animalKills=0,playerDir=0;boolean sword=true;
  long previous=System.nanoTime();
  Game(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);paint.setFilterBitmap(false);loadSprites();loadDecorations();spawnMobs();}
  void spawnMobs(){
   String[] animals={"boar","fox","deer","black_grouse"};
   for(int i=0;i<28;i++){float x=80+random.nextInt(1440),y=80+random.nextInt(840);if(!blocked(x,y))mobs.add(new Mob(animals[i%4],x,y));}
   for(int i=0;i<18;i++){float x=380+random.nextInt(1100),y=80+random.nextInt(840);if(!blocked(x,y))mobs.add(new Mob("orc"+(1+i%3),x,y));}
  }
  void loadSprites(){
   String[] kinds={"boar","fox","deer","black_grouse","orc1","orc2","orc3","player_sword","player_unarmed"};
   String[] actions={"idle","walk","run","attack","run_attack","walk_attack","flight","hurt","death"};
   String[] dirs={"front","back","left","right"};
   for(String k:kinds)for(String a:actions)for(String d:dirs){
    String key=k+"_"+a+"_"+d;
    try(InputStream in=getAssets().open("generated/"+key+".png")){Bitmap b=BitmapFactory.decodeStream(in);if(b!=null)sprites.put(key,b);}catch(Exception ignored){}
   }
  }
  Bitmap asset(String filename){
   try(InputStream in=getAssets().open("sprites/"+filename)){return BitmapFactory.decodeStream(in);}catch(Exception ignored){return null;}
  }
  void loadDecorations(){
   ArrayList<Bitmap> rocks=new ArrayList<>(),ruins=new ArrayList<>(),plants=new ArrayList<>();
   String[] ruinTypes={"Blue-gray_ruins","Brown-gray_ruins","Brown_ruins","Sand_ruins"};
   for(String type:ruinTypes)for(int i=1;i<=5;i++){Bitmap b=asset(type+i+".png");if(b!=null)ruins.add(b);}
   for(int type=1;type<=6;type++)for(int i=1;i<=5;i++){Bitmap b=asset("Rock"+type+"_"+i+".png");if(b!=null)rocks.add(b);}
   try{
    String[] filenames=getAssets().list("sprites");
    if(filenames!=null)for(String filename:filenames){
     String lower=filename.toLowerCase(java.util.Locale.US);
     if(lower.endsWith(".png")&&(lower.contains("bush")||lower.contains("cactus")||lower.contains("fern")||lower.contains("fran")||lower.contains("plant"))){
      Bitmap b=asset(filename);if(b!=null)plants.add(b);
     }
    }
   }catch(Exception ignored){}
   for(int i=0;i<19;i++){float x=300+(i*251)%1200,y=90+(i*197)%810;Bitmap b=ruins.isEmpty()?null:ruins.get(i%ruins.size());decorations.add(new Decoration(b,x,y,94,90,true));}
   for(int i=0;i<85;i++){float x=60+(i*167)%1480,y=65+(i*283)%870;if(Math.hypot(x-180,y-180)<115)continue;Bitmap b=rocks.isEmpty()?null:rocks.get(i%rocks.size());decorations.add(new Decoration(b,x,y,42+(i%3)*12,38+(i%4)*9,true));}
   for(int i=0;i<70&&!plants.isEmpty();i++){float x=30+(i*131)%1540,y=40+(i*223)%920;decorations.add(new Decoration(plants.get(i%plants.size()),x,y,34+(i%3)*12,40+(i%3)*10,false));}
  }
  boolean blocked(float x,float y){
   if(x<18||y<18||x>WORLD_W-18||y>WORLD_H-18)return true;
   for(Decoration d:decorations)if(d.solid&&Math.abs(x-d.x)<d.w*.34f+7&&Math.abs(y-d.y)<d.h*.24f+6)return true;
   return false;
  }
  void drawRect(int col,float x,float y,float w,float h){paint.setColor(col);canvas.drawRect(x,y,x+w,y+h,paint);}
  int col(String hex){return Color.parseColor(hex);}
  void drawSprite(String kind,String action,int dir,float x,float y,float w,float h,float time,boolean freeze){
   String direction=new String[]{"front","back","left","right"}[Math.max(0,Math.min(3,dir))];
   Bitmap b=sprites.get(kind+"_"+action+"_"+direction);
   if(b==null)b=sprites.get(kind+"_idle_"+direction);
   if(b==null){drawRect(kind.startsWith("orc")?col("#71994b"):kind.startsWith("player")?col("#4775a0"):col("#b98d62"),x-w/2,y-h/2,w,h);return;}
   int frameWidth=b.getHeight();int count=Math.max(1,b.getWidth()/Math.max(1,frameWidth));
   int frame=freeze?count-1:((int)(Math.max(0,time)*8))%count;
   paint.setColor(Color.WHITE);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,new Rect(frame*frameWidth,0,(frame+1)*frameWidth,b.getHeight()),new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);
  }
  void update(float dt){
   elapsed+=dt;attackCooldown=Math.max(0,attackCooldown-dt);attackAnim=Math.max(0,attackAnim-dt);hurtAnim=Math.max(0,hurtAnim-dt);
   if(hp<=0){deathAnim+=dt;return;}
   float mx=joyX,my=joyY,length=(float)Math.hypot(mx,my);
   if(length>1){mx/=length;my/=length;}
   if(Math.abs(mx)+Math.abs(my)>.08f){
    playerDir=Math.abs(mx)>Math.abs(my)?(mx<0?2:3):(my<0?1:0);
    float nx=px+mx*140*dt,ny=py+my*140*dt;
    if(!blocked(nx,py))px=nx;
    if(!blocked(px,ny))py=ny;
   }
   for(Mob m:mobs){
    if(m.dead){m.deathTimer+=dt;continue;}
    m.anim+=dt;m.attackTimer=Math.max(0,m.attackTimer-dt);m.hurtTimer=Math.max(0,m.hurtTimer-dt);
    float distance=(float)Math.hypot(px-m.x,py-m.y);
    float vx=0,vy=0;String action="idle";
    if(m.hurtTimer>0){action="hurt";}
    else if(m.hostile()&&distance<330){
     m.dir=direction(px-m.x,py-m.y);
     if(distance>43){vx=(px-m.x)/Math.max(1,distance)*75;vy=(py-m.y)/Math.max(1,distance)*75;action="run";}
     else {action="attack";if(m.attackTimer<=0){hp=Math.max(0,hp-1);hurtAnim=.32f;m.attackTimer=1.3f;}}
    }else if(!m.hostile()&&distance<110){
     vx=(m.x-px)/Math.max(1,distance)*105;vy=(m.y-py)/Math.max(1,distance)*105;
     action=m.kind.equals("black_grouse")?"flight":"run";
    }else{
     m.decision-=dt;
     if(m.decision<=0){m.decision=1.2f+random.nextFloat()*2.5f;float angle=random.nextFloat()*6.28318f;
      if(random.nextFloat()<.27f){m.vx=0;m.vy=0;}else{m.vx=(float)Math.cos(angle)*35;m.vy=(float)Math.sin(angle)*35;}
     }
     vx=m.vx;vy=m.vy;action=Math.abs(vx)+Math.abs(vy)<1?"idle":"walk";
    }
    if(Math.abs(vx)+Math.abs(vy)>1){
     float nx=m.x+vx*dt,ny=m.y+vy*dt;
     if(!blocked(nx,m.y))m.x=nx;else m.decision=0;
     if(!blocked(m.x,ny))m.y=ny;else m.decision=0;
     m.dir=direction(vx,vy);
    }
    if(!m.action.equals(action)){m.action=action;m.anim=0;}
   }
  }
  int direction(float vx,float vy){return Math.abs(vx)>Math.abs(vy)?(vx<0?2:3):(vy<0?1:0);}
  void attack(){
   if(hp<=0||attackCooldown>0)return;
   attackCooldown=.42f;attackAnim=.4f;
   float fx=playerDir==2?-1:playerDir==3?1:0,fy=playerDir==1?-1:playerDir==0?1:0;
   for(Mob m:mobs){
    if(m.dead)continue;
    float dx=m.x-px,dy=m.y-py,d=(float)Math.hypot(dx,dy);
    if(d>76||d<.01f)continue;
    if((dx*fx+dy*fy)/d<-.1f)continue;
    m.hp-=sword?2:1;m.hurtTimer=.28f;m.anim=0;
    if(m.hp<=0){m.dead=true;m.deathTimer=0;if(m.hostile())kills++;else animalKills++;}
   }
  }
  void restart(){
   hp=maxHp;px=180;py=180;joyX=joyY=0;deathAnim=0;kills=animalKills=0;mobs.clear();spawnMobs();
  }
  @Override protected void onDraw(Canvas c){
   canvas=c;float dt=Math.min(.05f,(System.nanoTime()-previous)/1000000000f);previous=System.nanoTime();update(dt);
   float w=getWidth(),h=getHeight(),scale=Math.min(w/800f,h/450f),vw=w/scale,vh=h/scale;
   c.drawColor(col("#182b24"));c.save();c.scale(scale,scale);c.save();c.translate(vw/2-px,vh/2-py);
   for(int x=0;x<WORLD_W;x+=32)for(int y=0;y<WORLD_H;y+=32){
    int n=(x*37+y*19)%7;drawRect(n<3?col("#407d46"):col("#47884a"),x,y,32,32);
    if(n==1)drawRect(col("#6a9a53"),x+8,y+7,5,4);
   }
   for(Decoration d:decorations){
    if(d.image!=null){paint.setColor(Color.WHITE);c.drawBitmap(d.image,null,new RectF(d.x-d.w/2,d.y-d.h*.8f,d.x+d.w/2,d.y+d.h*.2f),paint);}
    else drawRect(col("#82776b"),d.x-d.w/2,d.y-d.h/2,d.w,d.h);
   }
   for(Mob m:mobs){
    if(m.dead&&m.deathTimer>2.5f)continue;
    String action=m.dead?"death":m.hurtTimer>0?"hurt":m.action;
    float size=m.hostile()?68:50;
    drawSprite(m.kind,action,m.dir,m.x,m.y,size,size,m.dead?m.deathTimer:m.anim,m.dead&&m.deathTimer>.7f);
    if(!m.dead&&m.hostile()){drawRect(col("#352827"),m.x-20,m.y-37,40,5);drawRect(col("#d65c4e"),m.x-20,m.y-37,40f*m.hp/m.maxHp,5);}
   }
   String playerKind=sword?"player_sword":"player_unarmed";
   String playerAction=hp<=0?"death":hurtAnim>0?"hurt":attackAnim>0&&sword?"attack":Math.abs(joyX)+Math.abs(joyY)>.08f?"run":"idle";
   drawSprite(playerKind,playerAction,playerDir,px,py,64,64,hp<=0?deathAnim:attackAnim>0?.4f-attackAnim:elapsed,hp<=0&&deathAnim>.8f);
   c.restore();
   drawRect(col("#142b24"),0,0,vw,37);paint.setColor(Color.WHITE);paint.setTextSize(18);
   c.drawText("HP "+hp+"/"+maxHp+"   Orks "+kills+"   Tiere "+animalKills+"   "+(sword?"Schwert":"Unbewaffnet"),12,25,paint);
   drawRect(col("#263d40"),18,vh-112,100,98);drawRect(col("#496b67"),61,vh-102,14,78);drawRect(col("#496b67"),28,vh-70,82,14);
   drawRect(col("#a54c45"),vw-110,vh-104,90,90);paint.setColor(Color.WHITE);paint.setTextSize(21);c.drawText("HIT",vw-87,vh-52,paint);
   drawRect(col("#394e69"),vw-225,vh-86,102,56);paint.setTextSize(14);paint.setColor(Color.WHITE);c.drawText(sword?"WAFFE":"SCHWERT",vw-216,vh-52,paint);
   if(hp<=0){drawRect(col("#111111"),vw/2-180,vh/2-45,360,90);paint.setColor(Color.WHITE);paint.setTextSize(24);c.drawText("GAME OVER - Tippen",vw/2-145,vh/2+7,paint);}
   c.restore();postInvalidateDelayed(16);
  }
  @Override public boolean onTouchEvent(MotionEvent e){
   float scale=Math.min(getWidth()/800f,getHeight()/450f),x=e.getX()/scale,y=e.getY()/scale,vw=getWidth()/scale,vh=getHeight()/scale;
   int type=e.getActionMasked();
   if(type==MotionEvent.ACTION_UP||type==MotionEvent.ACTION_CANCEL){joyX=joyY=0;return true;}
   if(hp<=0){if(type==MotionEvent.ACTION_DOWN)restart();return true;}
   if(x>vw-118&&y>vh-118){if(type==MotionEvent.ACTION_DOWN)attack();}
   else if(x>vw-235&&x<vw-115&&y>vh-105){if(type==MotionEvent.ACTION_DOWN)sword=!sword;}
   else if(x<145&&y>vh-145){joyX=Math.max(-1,Math.min(1,(x-68)/36));joyY=Math.max(-1,Math.min(1,(y-(vh-65))/36));}
   return true;
  }
  @Override public boolean onKeyDown(int key,KeyEvent e){
   switch(key){case KeyEvent.KEYCODE_A:case KeyEvent.KEYCODE_DPAD_LEFT:joyX=-1;break;
    case KeyEvent.KEYCODE_D:case KeyEvent.KEYCODE_DPAD_RIGHT:joyX=1;break;
    case KeyEvent.KEYCODE_W:case KeyEvent.KEYCODE_DPAD_UP:joyY=-1;break;
    case KeyEvent.KEYCODE_S:case KeyEvent.KEYCODE_DPAD_DOWN:joyY=1;break;
    case KeyEvent.KEYCODE_SPACE:attack();break;
    case KeyEvent.KEYCODE_E:sword=!sword;break;
    case KeyEvent.KEYCODE_ENTER:if(hp<=0)restart();break;
   }return true;
  }
  @Override public boolean onKeyUp(int key,KeyEvent e){
   if(key==KeyEvent.KEYCODE_A||key==KeyEvent.KEYCODE_D||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)joyX=0;
   if(key==KeyEvent.KEYCODE_W||key==KeyEvent.KEYCODE_S||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)joyY=0;
   return true;
  }
 }
}
