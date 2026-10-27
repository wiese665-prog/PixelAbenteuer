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
  String kind,action="idle";float x,y,vx,vy,anim=0,decision=0,attackTimer=0,hurtTimer=0,deathTimer=0,healthVisible=0;int hp,maxHp,level,damage,dir=0;boolean dead=false;float damageNumberTime=0;int lastDamage=0;
  Mob(String k,float xx,float yy) {kind=k;x=xx;y=yy;level=1+(int)(Math.random()*(k.startsWith("orc")?6:4));int baseHp=k.equals("orc3")?12:k.equals("orc2")?9:k.equals("orc1")?7:k.equals("boar")?8:k.equals("deer")?5:k.equals("fox")?4:2;maxHp=baseHp+(level-1)*(k.equals("boar")||k.startsWith("orc")?3:2);hp=maxHp;damage=(k.equals("orc3")?3:k.equals("orc2")?2:k.equals("orc1")?1:0)+(level-1)/3;}
  boolean hostile(){return kind.startsWith("orc");}
 }
 class Decoration {
  Bitmap image,mask;float x,y,w,h;boolean solid;
  Decoration(Bitmap b,Bitmap collision,float xx,float yy,float ww,float hh,boolean s){image=b;mask=collision==null?b:collision;x=xx;y=yy;w=ww;h=hh;solid=s;}
 }
 class Drop {String item;float x,y,bob;Drop(String i,float xx,float yy,float t){item=i;x=xx;y=yy;bob=t;}}
 class Item {
  String name,rarity,slot;int dmg,armor,hp,heal,mana;
  Item(String n,String rar,String sl,int d,int a,int h,int healing){name=n;rarity=rar;slot=sl;dmg=d;armor=a;hp=h;heal=healing;mana=0;}
  boolean equipment(){return !slot.equals("");}
 }
 class Game extends View {
  final int WORLD_W=1600,WORLD_H=1000;
  Canvas canvas;Paint paint=new Paint();Random random=new Random(42);
  ArrayList<Mob> mobs=new ArrayList<>();ArrayList<Decoration> decorations=new ArrayList<>();ArrayList<Drop> drops=new ArrayList<>();LinkedHashMap<String,Integer> inventory=new LinkedHashMap<>();HashMap<String,Bitmap> itemImages=new HashMap<>();
  HashMap<String,Item> definitions=new HashMap<>();HashMap<String,String> equipped=new HashMap<>();
  String[] hotbar=new String[5];int selectedHotbar=-1;int copper=0,silver=0,gold=0,hoverSlot=-1;float hoverX=0,hoverY=0;
  boolean inventoryOpen=false,shopOpen=false,startScreen=true,pauseMenu=false;float merchantX=265,merchantY=215;
  String[] shopStock={"Apple","Ham","Healthpotion","Healpotion med","Iron Sword","Silver Sword","Leather Armor","Iron Armor","Iron Helmet","Golden Sword"};
  String pickupNotice="";float noticeTime=0;
  HashMap<String,Bitmap> sprites=new HashMap<>();
  float px=180,py=180,joyX=0,joyY=0,elapsed=0,worldTime=0,attackAnim=0,hurtAnim=0,deathAnim=0,attackCooldown=0,playerHealthVisible=0;
  int hp=12,maxHp=12,kills=0,animalKills=0,playerDir=0,playerLevel=1,exp=0,attackPower=3,lastPlayerDamage=0;float playerDamageTime=0;float swordTimer=0;boolean sword=false;
  long previous=System.nanoTime();
  Game(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);paint.setFilterBitmap(false);loadSprites();loadBuildings();loadClouds();loadItems();loadDecorations();spawnMobs();}
  void spawnMobs(){
   String[] animals={"boar","fox","deer","black_grouse"};
   for(int i=0;i<28;i++){float x=80+random.nextInt(1440),y=80+random.nextInt(840);if(!blocked(x,y)&&!safeZone(x,y)&&Math.hypot(x-merchantX,y-merchantY)>110)mobs.add(new Mob(animals[i%4],x,y));}
   for(int i=0;i<18;i++){float x=380+random.nextInt(1100),y=80+random.nextInt(840);if(!blocked(x,y)&&!safeZone(x,y)&&Math.hypot(x-merchantX,y-merchantY)>110)mobs.add(new Mob("orc"+(1+i%3),x,y));}
  }
  final ArrayList<Bitmap[]>[] cloudAnimations=new ArrayList[]{new ArrayList<Bitmap[]>(),new ArrayList<Bitmap[]>(),new ArrayList<Bitmap[]>()};
  void loadClouds(){
   String[] families={"Cloud","Cloud1","Cloud2"};
   for(int familyIndex=0;familyIndex<families.length;familyIndex++)for(int shape=2;shape<=5;shape++){
    String family=families[familyIndex];
    Bitmap[] frames=new Bitmap[5];boolean valid=true;
    for(int f=1;f<=5;f++){
     String file="sprites/"+family+"Shape"+shape+"/cloud_shape"+shape+"_"+f+".png";
     try(InputStream in=getAssets().open(file)){frames[f-1]=BitmapFactory.decodeStream(in);}
     catch(Exception ex){valid=false;break;}
     if(frames[f-1]==null){valid=false;break;}
    }
    if(valid)cloudAnimations[familyIndex].add(frames);
   }
  }
  void drawClouds(float vw,float vh){
   if(cloudAnimations[0].isEmpty()&&cloudAnimations[1].isEmpty()&&cloudAnimations[2].isEmpty())return;
   float phase=(worldTime%240f)/240f;
   float night=(float)Math.max(0,Math.min(1,(-Math.cos(phase*6.2831853f)-.10f)/.85f));
   // Clouds always have 30% transparency (70% opacity), regardless of time of day.
   final int alpha=179;
   // Screen-space cloud shadows drift across the landscape independently of the camera.
   for(int i=0;i<11;i++){
    // Day: white Cloud + grey Cloud2. Night: black Cloud1 + grey Cloud2.
    // Fade the changing family smoothly at sunrise and sunset.
    int family=(i%2==0)?2:(night>=.5f?1:0);
    float familyOpacity=(i%2==0)?1f:(night>=.5f?night:1f-night);
    if(cloudAnimations[family].isEmpty())continue;
    Bitmap[] frames=cloudAnimations[family].get((i/2)%cloudAnimations[family].size());
    int frame=(int)(worldTime*.9f+i*2)%frames.length;
    Bitmap cloud=frames[frame];if(cloud==null)continue;
    float width=100+(i%4)*35,height=width*cloud.getHeight()/Math.max(1f,cloud.getWidth());
    height=Math.min(95,height);
    float x=((i*173+worldTime*(8+i%3*3))%(vw+width*2))-width;
    float y=38+(i*79)%(int)Math.max(70,vh-95);
    // Screen center is the player position; soften clouds when covering the character.
    float playerLeft=vw/2-28,playerTop=vh/2-44;
    boolean overPlayer=x<playerLeft+56&&x+width>playerLeft&&y<playerTop+88&&y+height>playerTop;
    float overlapOpacity=overPlayer?.20f:1f; // 80% more transparent on contact.
    paint.setColor(Color.WHITE);paint.setAlpha((int)(alpha*familyOpacity*overlapOpacity));paint.setColorFilter(null);
    if(!indoors&&Math.hypot(px-forgeX,py-(forgeY+85))<160)paint.setAlpha(Math.round(paint.getAlpha()*.30f));
    canvas.drawBitmap(cloud,null,new RectF(x,y,x+width,y+height),paint);
   }
   paint.setAlpha(255);
  }
  final HashMap<String,Bitmap> buildingSprites=new HashMap<>();
  boolean indoors=false;int currentHouse=0;
  final float forgeX=550,forgeY=300,homeX=300,homeY=500;
  boolean safeZone(float x,float y){return Math.hypot(x-forgeX,y-forgeY)<340;}
  void loadBuildings(){
   for(String name:new String[]{"Exterior_house","Walls_interior","Walls_street","Interior_objects","Doors_windows_animations","Decorative_cracks","Forge","Light_animation","Master_Idle","Master","Seller","Customer"}){
    try(InputStream in=getAssets().open("sprites/"+name+".png")){
     Bitmap b=BitmapFactory.decodeStream(in);if(b!=null)buildingSprites.put(name,b);
    }catch(Exception ignored){}
   }
  }
  void tile(String name,int sx,int sy,int sw,int sh,float x,float y,float w,float h){
   Bitmap bitmap=buildingSprites.get(name);if(bitmap==null)return;
   sx=Math.max(0,Math.min(sx,bitmap.getWidth()-1));sy=Math.max(0,Math.min(sy,bitmap.getHeight()-1));
   sw=Math.min(sw,bitmap.getWidth()-sx);sh=Math.min(sh,bitmap.getHeight()-sy);
   paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setColorFilter(null);
   canvas.drawBitmap(bitmap,new Rect(sx,sy,sx+sw,sy+sh),new RectF(x,y,x+w,y+h),paint);
  }
  void characterTile(String name,int columns,int rows,int frame,float x,float y,float size){
   Bitmap bitmap=buildingSprites.get(name);if(bitmap==null)return;
   int fw=bitmap.getWidth()/columns,fh=bitmap.getHeight()/rows;
   int index=Math.floorMod(frame,columns*rows),sx=(index%columns)*fw,sy=(index/columns)*fh;
   // Trim transparent padding inside the selected animation cell so all NPCs
   // have the same visible height as the player, regardless of sheet spacing.
   int minX=fw,minY=fh,maxX=-1,maxY=-1;
   for(int yy=0;yy<fh;yy++)for(int xx=0;xx<fw;xx++){
    if(Color.alpha(bitmap.getPixel(sx+xx,sy+yy))>32){
     if(xx<minX)minX=xx;if(xx>maxX)maxX=xx;
     if(yy<minY)minY=yy;if(yy>maxY)maxY=yy;
    }
   }
   if(maxX<minX||maxY<minY)return;
   float h=size*.78f,w=h*(maxX-minX+1)/(float)(maxY-minY+1);
   w=Math.min(w,size*.85f);
   tile(name,sx+minX,sy+minY,maxX-minX+1,maxY-minY+1,x-w/2,y-h,w,h);
  }
  void sellerPortrait(float x,float y){
   Bitmap bitmap=buildingSprites.get("Seller");if(bitmap==null)return;
   // Seller.png is 96x96: each of the 3x2 cells is 32x48.
   // Its 48px-high cells contain an unwanted second row fragment.
   // Isolate the first standing character within the upper 32x32 area.
   int sx=0,sy=0,sw=Math.min(32,bitmap.getWidth()),sh=Math.min(32,bitmap.getHeight());
   int minX=sw,minY=sh,maxX=-1,maxY=-1;
   for(int yy=0;yy<sh;yy++)for(int xx=0;xx<sw;xx++)
    if(Color.alpha(bitmap.getPixel(sx+xx,sy+yy))>32){
     minX=Math.min(minX,xx);minY=Math.min(minY,yy);
     maxX=Math.max(maxX,xx);maxY=Math.max(maxY,yy);
    }
   if(maxX>=minX&&maxY>=minY){
    float h=31,w=Math.min(26,h*(maxX-minX+1)/(float)(maxY-minY+1));
    tile("Seller",sx+minX,sy+minY,maxX-minX+1,maxY-minY+1,x-w/2,y-h,w,h);
   }
  }
  void drawHouse(float x,float y,boolean smithy){
   // Exact source rectangles measured against Exterior_house.png (400x208).
   // The facade is a complete single sprite in x=0..145, y=0..162.
   tile("Exterior_house",0,0,145,163,x-116,y-140,232,261);
   // The loose props are separate cells on the bottom of the same atlas.
   tile("Exterior_house",70,162,32,38,x-117,y+113,50,52); // crates
   tile("Exterior_house",116,160,34,34,x+97,y+66,55,55); // complete cart
   paint.setColor(col("#fff0ce"));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(13);
   canvas.drawText("SCHMIEDE",x,y-150,paint);paint.setTextAlign(Paint.Align.LEFT);
  }
  // Door anchors are shared by the automatic movement triggers and teleportation.
  final float DOOR_OUT_X=0f,DOOR_OUT_Y=102f;
  final float DOOR_IN_X=555f,DOOR_IN_Y=480f;
  void enterOrLeave(){
   if(indoors){
    indoors=false;px=forgeX+DOOR_OUT_X;py=forgeY+DOOR_OUT_Y+43f;
    joyX=joyY=0;return;
   }
   if(Math.abs(px-(forgeX+DOOR_OUT_X))<22&&
      py>forgeY+DOOR_OUT_Y-22&&py<forgeY+DOOR_OUT_Y+10){
    indoors=true;currentHouse=1;px=DOOR_IN_X;py=DOOR_IN_Y-22f;
    joyX=joyY=0;
   }
  }
  void drawInterior(float vw,float vh){
   float l=vw/2-340,t=vh/2-176;
   drawRect(col("#18141a"),0,0,vw,vh);
   // Gray square paving from the dedicated right-hand tile strip of Walls_interior.
   for(int iy=0;iy<18;iy++)for(int ix=0;ix<20;ix++)
    tile("Walls_interior",240,128,16,16,l+16+ix*16,t+52+iy*16,16,16);
   // Keep the approved wooden sales floor.
   for(int iy=0;iy<18;iy++)for(int ix=0;ix<20;ix++)
    tile("Walls_interior",145,21,12,12,l+340+ix*16,t+52+iy*16,16,16);
   // Original wall blocks at their native proportions; no stretched long strip.
   for(int ix=0;ix<14;ix++)
    tile("Walls_interior",48+(ix%2)*32,0,32,32,l+16+ix*46,t,46,70);
   for(int iy=0;iy<6;iy++){
    tile("Walls_interior",49,128,23,42,l,t+67+iy*44,22,44);
    tile("Walls_interior",193,128,23,42,l+658,t+67+iy*44,22,44);
   }
   // Divider: two original stone piers with a broad passable gap.
   tile("Walls_interior",49,128,23,42,l+316,t+52,22,161);
   // No lower partition: leave the forge/shop walkway open.
   // Floors meet directly beneath the stone divider; no overlaid floor strip.
   // Forge: show one complete furnace cell, not half an animation sheet.
   Bitmap furnace=buildingSprites.get("Forge");
   if(furnace!=null){
    // Forge.png is a 128x192 sheet of six 64x64 frames.
    // Crop exactly one frame, trimming transparent margins without crossing
    // into adjacent frames or cutting off any opaque furnace pixels.
    int fw=Math.min(64,furnace.getWidth()),fh=Math.min(64,furnace.getHeight());
    int left=fw,top=fh,right=-1,bottom=-1;
    for(int fy=0;fy<fh;fy++)for(int fx=0;fx<fw;fx++){
     if(Color.alpha(furnace.getPixel(fx,fy))>0){
      left=Math.min(left,fx);top=Math.min(top,fy);
      right=Math.max(right,fx);bottom=Math.max(bottom,fy);
     }
    }
    if(right>=left&&bottom>=top){
     int cw=right-left+1,ch=bottom-top+1;
     float scale=Math.min(102.6f/cw,109.8f/ch);
     float dw=cw*scale,dh=ch*scale;
     tile("Forge",left,top,cw,ch,l+75+(114-dw)/2,t+40+(122-dh),dw,dh);
    }
   }
   // One complete original ore crate, no clipped fragments at entrance.
   
   // Two complete rear display shelves and one dragon picture.
   tile("Interior_objects",13,128,34,51,l+381,t+95,53,68);
   tile("Interior_objects",113,129,34,51,l+570,t+95,53,68);
   tile("Interior_objects",114,257,59,49,l+451,t+82,106,59);
   // Two separate COMPLETE purple rugs (source x=6..60, y=397..421).
   tile("Interior_objects",0,385,59,40,l+415,t+156,205,72);
   tile("Interior_objects",70,385,55,40,l+415,t+256,205,72);
   // The complete counter with its decorative vases, cropped as one object.
   tile("Interior_objects",16,0,160,27,l+345.5f,t+213,309,46);
   // Two complete decorative original sprites at the bottom of the forge.
   tile("Interior_objects",0,328,65,60,l+69,t+265,64,65);
   tile("Interior_objects",70,328,47,55,l+211,t+270,51,58);
   // Additional forge details from distinct atlas cells, not duplicated crates.
   tile("Interior_objects",8,328,45,56,l+135,t+270,47,58);
   tile("Interior_objects",70,328,47,55,l+271,t+270,47,58);
   // Complete stair-display shelves on the bottom wall, flanking the entry.
   // Door is at interior x=555 (screen x=l+495); keep a clear central aisle.
   tile("Interior_objects",98,70,46,59,l+341,t+267,61,72);
   tile("Interior_objects",148,70,46,59,l+596,t+267,61,72);
   // The seller is drawn only once, using a single stable pose.
   characterTile("Master_Idle",3,2,(int)(elapsed*2)%3,l+236,t+175,81.4f);
   sellerPortrait(l+497,t+236);
   characterTile("Customer",3,2,0,l+507,t+288,52);
   float cx=l+340+(px-400),cy=t+176+(py-350);
   drawSprite(sword?"player_sword":"player_unarmed","idle",playerDir,cx,cy,94,94,elapsed,false);
   paint.setColor(col("#fff2d5"));paint.setTextSize(13);
   canvas.drawText("SCHMIEDE  |  Ausgang unten mittig",l+221,t+345,paint);
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
   ArrayList<Bitmap> rocks=new ArrayList<>(),rockMasks=new ArrayList<>(),ruins=new ArrayList<>(),plants=new ArrayList<>();
   String[] ruinTypes={"Blue-gray_ruins","Brown-gray_ruins","Brown_ruins","Sand_ruins"};
   for(String type:ruinTypes)for(int i=1;i<=5;i++){Bitmap b=asset(type+i+".png");if(b!=null)ruins.add(b);}
   for(int type=1;type<=6;type++)for(int i=1;i<=5;i++){Bitmap b=asset("Rock"+type+"_"+i+".png");if(b!=null){rocks.add(b);Bitmap mask=asset("Rock"+type+"_"+i+"_no_shadow.png");rockMasks.add(mask==null?b:mask);}}
   try{
    String[] filenames=getAssets().list("sprites");
    if(filenames!=null)for(String filename:filenames){
     String lower=filename.toLowerCase(java.util.Locale.US);
     if(lower.endsWith(".png")&&(lower.contains("bush")||lower.contains("cactus")||lower.contains("fern")||lower.contains("fran")||lower.contains("plant"))){
      Bitmap b=asset(filename);if(b!=null)plants.add(b);
     }
    }
   }catch(Exception ignored){}
   for(int i=0;i<19;i++){float x=300+(i*251)%1200,y=90+(i*197)%810;Bitmap b=ruins.isEmpty()?null:ruins.get(i%ruins.size());if(Math.hypot(x-forgeX,y-forgeY)<220)continue;decorations.add(new Decoration(b,b,x,y,94,90,true));}
   for(int i=0;i<85;i++){float x=60+(i*167)%1480,y=65+(i*283)%870;if(Math.hypot(x-180,y-180)<140||Math.hypot(x-forgeX,y-forgeY)<185)continue;Bitmap b=rocks.isEmpty()?null:rocks.get(i%rocks.size());decorations.add(new Decoration(b,rockMasks.isEmpty()?b:rockMasks.get(i%rockMasks.size()),x,y,20+(i%3)*4,19+(i%4)*4,true));}
   for(int i=0;i<70&&!plants.isEmpty();i++){float x=30+(i*131)%1540,y=40+(i*223)%920;if(Math.hypot(x-forgeX,y-forgeY)<185)continue;decorations.add(new Decoration(plants.get(i%plants.size()),plants.get(i%plants.size()),x,y,34+(i%3)*12,40+(i%3)*10,true));}
  }
  // Per-pixel collisions use the very same destination rectangle as rendering.
  // Test the feet/body of the moving character against opaque object pixels.
  boolean collides(Decoration d,float x,float y){
   Bitmap mask=d.mask;
   float left=d.x-d.w/2,top=d.y-d.h*.8f;
   if(x+10<left||x-10>left+d.w||y+12<top||y-12>top+d.h)return false;
   if(mask==null)return Math.abs(x-d.x)<d.w*.4f+9&&Math.abs(y-d.y)<d.h*.4f+9;
   int bw=mask.getWidth(),bh=mask.getHeight();
   for(int ix=-2;ix<=2;ix++)for(int iy=-2;iy<=2;iy++){
    float sampleX=x+ix*4f,sampleY=y+iy*4f;
    float u=(sampleX-left)/d.w,v=(sampleY-top)/d.h;
    if(u<0||u>=1||v<0||v>=1)continue;
    int sx=Math.min(bw-1,(int)(u*bw)),sy=Math.min(bh-1,(int)(v*bh));
    if(Color.alpha(mask.getPixel(sx,sy))>=200)return true;
   }
   return false;
  }
  boolean blocked(float x,float y){
   if(x<18||y<18||x>WORLD_W-18||y>WORLD_H-18)return true;
   for(Decoration d:decorations)if(d.solid&&Math.hypot(d.x-forgeX,d.y-forgeY)>185&&collides(d,x,y))return true;
   float dx=x-forgeX,dy=y-forgeY;
   if(Math.abs(dx)<110&&dy>-63&&dy<65&&!(Math.abs(dx-DOOR_OUT_X)<25&&dy>32))return true;
   if(dx>-122&&dx<-66&&dy>111&&dy<168)return true;
   // Keep the whole door approach clear; the old rectangle trapped the player.
   if(dx>12&&dx<44&&dy>79&&dy<110&&Math.abs(dx-DOOR_OUT_X)>26)return true;
   if(dx>3&&dx<36&&dy>109&&dy<127&&Math.abs(dx-DOOR_OUT_X)>26)return true;
   if(dx>91&&dx<139&&dy>48&&dy<93)return true;
   return false;
  }
  int expNeeded(){return 12+playerLevel*8;}
  void earnExp(int amount){
   exp+=amount;
   while(exp>=expNeeded()){
    exp-=expNeeded();playerLevel++;recalcStats();hp=Math.min(maxHp,hp+4);
    playerHealthVisible=3f;
   }
  }
  void damageText(float x,float y,int damage,float remaining){
   if(remaining<=0)return;
   float age=1f-remaining/.9f;
   float rise=age*31f,spread=(float)Math.sin(damage*7.1)*9f;
   paint.setTypeface(Typeface.create("monospace",Typeface.BOLD));
   paint.setTextSize(21f+5f*(1f-age));paint.setTextAlign(Paint.Align.CENTER);
   String label="-"+damage;float tx=x+spread,ty=y-rise;
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);paint.setColor(col("#382029"));
   canvas.drawText(label,tx,ty,paint);
   paint.setStyle(Paint.Style.FILL);paint.setColor(col("#ffbc58"));canvas.drawText(label,tx,ty,paint);
   paint.setColor(col("#fff2ae"));canvas.drawText(label,tx-1,ty-2,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);
  }
  int rarityColor(String rarity){
   if("Legendär".equals(rarity))return col("#f6c95b");
   if("Super Rare".equals(rarity))return col("#c17af4");
   if("Rare".equals(rarity))return col("#61a8ff");
   if("Uncommon".equals(rarity))return col("#70d889");
   return col("#b9bdc2");
  }
  void define(String name,String rarity,String slot,int dmg,int armor,int bonusHp,int heal){
   definitions.put(name,new Item(name,rarity,slot,dmg,armor,bonusHp,heal));
  }
  void loadItems(){
   String[] filenames={"Healthpotion.png","Healthpotion large.png","Healpotion med.png","Green Apple.png","Ham.png","Apple.png","Mushroom.png","Cheese.png","Bag.png","Belt.png","Helm.png","Iron Armor.png","Iron Boot.png","Iron Helmet.png","Leather Armor.png","Leather Boot.png","Leather Helmet.png","Wizard Hat.png","Wooden Armor.png","Wood Log.png","Wool.png","Copper Coin.png","Gear.png","Golden Coin.png","Silver Coin.png","Rune Stone.png","Map.png","Bone.png","Egg.png","Feather.png","Monster Egg.png","Monster Eye.png","Monster Meat.png","Skull.png","Slime Gel.png","Coal.png","Copper Ingot.png","Copper Nugget.png","Crystal.png","Cut Emerald.png","Cut Ruby.png","Cut Sapphire.png","Cut Topaz.png","Diamond.png","Emerald.png","Gold Nugget.png","Golden Ingot.png","Obsidian.png","Pearl.png","Ruby.png","Sapphire.png","Silver Ingot.png","Silver Nugget.png","Topaz.png","Iron Sword.png","Silver Sword.png","Wooden Sword.png","Golden Sword.png"};
   for(String filename:filenames){Bitmap b=asset(filename);if(b!=null)itemImages.put(filename.substring(0,filename.length()-4),b);}
   define("Wooden Sword","Common","Waffe",1,0,0,0);
   define("Iron Sword","Uncommon","Waffe",3,0,0,0);
   define("Silver Sword","Rare","Waffe",6,0,0,0);
   define("Golden Sword","Legendär","Waffe",12,0,0,0);
   define("Wooden Armor","Common","Brust",0,1,1,0);
   define("Leather Armor","Uncommon","Brust",0,2,3,0);
   define("Iron Armor","Rare","Brust",0,6,8,0);
   define("Leather Helmet","Common","Kopf",0,1,1,0);
   define("Iron Helmet","Rare","Kopf",0,5,5,0);
   define("Wizard Hat","Super Rare","Kopf",4,2,8,0);
   define("Leather Boot","Common","Fuesse",0,1,0,0);
   define("Iron Boot","Rare","Fuesse",0,4,4,0);
   define("Belt","Uncommon","Guertel",0,1,2,0);
   define("Helm","Uncommon","Kopf",0,2,2,0);
   define("Bag","Common","Guertel",0,0,2,0);
   define("Apple","Common","",0,0,0,3);
   define("Green Apple","Common","",0,0,0,4);
   define("Mushroom","Common","",0,0,0,2);
   define("Cheese","Common","",0,0,0,5);
   define("Ham","Uncommon","",0,0,0,8);
   define("Monster Meat","Uncommon","",0,0,0,6);
   define("Healthpotion","Uncommon","",0,0,0,10);
   define("Healpotion med","Rare","",0,0,0,20);
   define("Healthpotion large","Super Rare","",0,0,0,40);
   setManaBonuses();
  }
  void itemIcon(String item,float x,float y,float scale){
   Bitmap b=itemImages.get(item);
   if(b==null)return;
   paint.setColor(Color.WHITE);paint.setColorFilter(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,null,new RectF(x,y,x+24*scale,y+24*scale),paint);
  }
  int price(String name){
   Item i=definitions.get(name);
   if(i==null)return 3;
   if(i.equipment())return i.rarity.equals("Legendär")?2500:i.rarity.equals("Super Rare")?900:i.rarity.equals("Rare")?320:i.rarity.equals("Uncommon")?95:30;
   return i.heal>0?Math.max(6,i.heal*4):5;
  }
  int wealth(){return gold*10000+silver*100+copper;}
  void setWealth(int value){value=Math.max(0,value);gold=value/10000;silver=(value/100)%100;copper=value%100;}
  void trade(String name,boolean buy){
   int cost=price(name);
   if(buy){if(wealth()<cost){pickupNotice="Nicht genug Muenzen";noticeTime=2;return;}
    setWealth(wealth()-cost);inventory.put(name,inventory.getOrDefault(name,0)+1);
    pickupNotice=name+" gekauft";
   }else{if(inventory.getOrDefault(name,0)<1)return;
    int count=inventory.get(name)-1;if(count<=0)inventory.remove(name);else inventory.put(name,count);
    setWealth(wealth()+Math.max(1,cost/2));cleanupHotbar();pickupNotice=name+" verkauft";
   }
   noticeTime=2;
  }
  void openShop(){if(Math.hypot(px-merchantX,py-merchantY)<100){shopOpen=true;inventoryOpen=false;hoverSlot=-1;}}
  int armorValue(){int total=0;for(String name:equipped.values()){Item i=definitions.get(name);if(i!=null)total+=i.armor;}return total;}
  int bonusHealth(){int total=0;for(String name:equipped.values()){Item i=definitions.get(name);if(i!=null)total+=i.hp;}return total;}
  int maxMana=20,mana=20;
  int bonusMana(){int total=0;for(String name:equipped.values()){Item i=definitions.get(name);if(i!=null)total+=i.mana;}return total;}
  void setManaBonuses(){
   definitions.get("Wizard Hat").mana=20;
   definitions.get("Helm").mana=5;
   definitions.get("Iron Helmet").mana=3;
   definitions.get("Belt").mana=4;
   definitions.get("Bag").mana=2;
  }
  void recalcStats(){
   int oldMax=maxHp,oldMana=maxMana;maxHp=12+(playerLevel-1)*4+bonusHealth();maxMana=20+(playerLevel-1)*2+bonusMana();mana=Math.min(maxMana,mana+Math.max(0,maxMana-oldMana));
   hp=Math.min(maxHp,hp+Math.max(0,maxHp-oldMax));
   attackPower=3+(playerLevel-1);
   Item weapon=definitions.get(equipped.get("Waffe"));if(weapon!=null)attackPower+=weapon.dmg;
   for(String name:equipped.values()){Item i=definitions.get(name);if(i!=null&&!i.slot.equals("Waffe"))attackPower+=i.dmg;}
  }
  void cleanupHotbar(){
   for(int i=0;i<hotbar.length;i++)if(hotbar[i]!=null&&inventory.getOrDefault(hotbar[i],0)<=0)hotbar[i]=null;
  }
  void assignHotbar(String name){
   Item item=definitions.get(name);if(item==null||item.heal<=0||inventory.getOrDefault(name,0)<=0)return;
   int slot=selectedHotbar;
   if(slot<0){for(int i=0;i<hotbar.length;i++)if(name.equals(hotbar[i])){slot=i;break;}}
   if(slot<0){for(int i=0;i<hotbar.length;i++)if(hotbar[i]==null){slot=i;break;}}
   if(slot<0)slot=0;
   hotbar[slot]=name;selectedHotbar=-1;pickupNotice=name+" auf Hotbar "+(slot+1);noticeTime=2f;
  }
  int hotbarIndexAt(float x,float y,float vw,float vh){
   float dx=x-(vw/2-130),dy=y-(vh-55);
   if(dx<0||dy<0||dy>50||dx>=265)return -1;
   int slot=(int)(dx/53);return dx%53<50?slot:-1;
  }
  void equip(String name){
   Item item=definitions.get(name);if(item==null||!item.equipment()||inventory.getOrDefault(name,0)<=0)return;
   String old=equipped.get(item.slot);if(name.equals(old))return;
   inventory.put(name,inventory.get(name)-1);if(inventory.get(name)<=0)inventory.remove(name);
   if(old!=null)inventory.put(old,inventory.getOrDefault(old,0)+1);
   equipped.put(item.slot,name);cleanupHotbar();recalcStats();hoverSlot=-1;
   pickupNotice=name+" angelegt";noticeTime=2f;
  }
  void unequip(String slot){
   String old=equipped.remove(slot);if(old==null)return;
   inventory.put(old,inventory.getOrDefault(old,0)+1);recalcStats();
   pickupNotice=old+" ausgezogen";noticeTime=2f;
  }
  int equippedSlotAt(float x,float y,float vw,float vh){
   float left=invLeft(vw),top=invTop(vh);
   float[][] p={{left+129,top+97},{left+21,top+155},{left+222,top+155},{left+21,top+246},{left+222,top+246}};
   for(int i=0;i<p.length;i++)if(x>=p[i][0]&&x<=p[i][0]+43&&y>=p[i][1]&&y<=p[i][1]+43)return i;
   return -1;
  }
  void useItem(String name){
   Item item=definitions.get(name);
   if(item==null||item.heal<=0||inventory.getOrDefault(name,0)<=0||hp<=0||hp>=maxHp)return;
   hp=Math.min(maxHp,hp+item.heal);inventory.put(name,inventory.get(name)-1);
   if(inventory.get(name)<=0)inventory.remove(name);
   cleanupHotbar();
   pickupNotice="+"+item.heal+" HP";noticeTime=1.6f;playerHealthVisible=3f;
  }
  void useHotbar(int index){if(index>=0&&index<hotbar.length&&hotbar[index]!=null)useItem(hotbar[index]);}
  void giveCoins(Mob m){
   int value=(m.hostile()?5:1)+m.level*(m.hostile()?4:2)+random.nextInt(m.hostile()?12:5);
   copper+=value;silver+=copper/100;copper%=100;gold+=silver/100;silver%=100;
  }
  void addDrop(String item,float x,float y){
   drops.add(new Drop(item,x+(random.nextFloat()-.5f)*22,y+(random.nextFloat()-.5f)*22,random.nextFloat()*6f));
  }
  void loot(Mob m){
   giveCoins(m);
   if(m.kind.equals("black_grouse")){
    addDrop("Feather",m.x,m.y);if(random.nextFloat()<.7f)addDrop("Egg",m.x,m.y);
   }else if(!m.hostile()){
    addDrop("Ham",m.x,m.y);
    if(random.nextFloat()<.65f)addDrop("Wool",m.x,m.y);
    if(m.kind.equals("boar")&&random.nextFloat()<.3f)addDrop("Bone",m.x,m.y);
   }else{
    String[] materials={"Bone","Skull","Monster Eye","Monster Meat","Rune Stone","Copper Nugget","Slime Gel"};
    addDrop(materials[random.nextInt(materials.length)],m.x,m.y);
    if(random.nextFloat()<.32f){
     String[] weapons={"Wooden Sword","Iron Sword","Silver Sword","Golden Sword"};
     float chance=random.nextFloat();
     addDrop(weapons[chance<.58f?0:chance<.85f?1:chance<.97f?2:3],m.x,m.y);
    }
    if(random.nextFloat()<.28f){
     String[] armor={"Wooden Armor","Leather Armor","Iron Armor","Leather Helmet","Iron Helmet","Leather Boot","Iron Boot","Wizard Hat","Belt","Helm","Bag"};
     addDrop(armor[random.nextInt(armor.length)],m.x,m.y);
    }
    if(random.nextFloat()<.23f){
     String[] food={"Apple","Green Apple","Cheese","Mushroom","Healthpotion","Healpotion med","Healthpotion large"};
     addDrop(food[random.nextInt(food.length)],m.x,m.y);
    }
   }
  }
  Drop nearestDrop(){
   Drop nearest=null;float best=65;
   for(Drop d:drops){float distance=(float)Math.hypot(px-d.x,py-d.y);if(distance<best){best=distance;nearest=d;}}
   return nearest;
  }
  void pickup(){
   if(inventoryOpen||shopOpen||pauseMenu||startScreen)return;
   Drop d=nearestDrop();if(d==null)return;
   inventory.put(d.item,inventory.containsKey(d.item)?inventory.get(d.item)+1:1);
   pickupNotice="+1 "+d.item;noticeTime=1.8f;drops.remove(d);
  }
  String[] inventoryNames(){return inventory.keySet().toArray(new String[0]);}
  void panel(float x,float y,float w,float h){
   drawRect(col("#171b24"),x-5,y-5,w+10,h+10);
   drawRect(col("#b59a68"),x-3,y-3,w+6,h+6);
   drawRect(col("#26323a"),x,y,w,h);
   drawRect(col("#34454c"),x+4,y+4,w-8,3);
  }
  void slotFrame(float x,float y,float size,String item){
   drawRect(col("#111c23"),x-2,y-2,size+4,size+4);
   drawRect(col("#65737b"),x,y,size,size);
   drawRect(col("#29333d"),x+3,y+3,size-6,size-6);
   if(item!=null){Item i=definitions.get(item);if(i!=null&&i.equipment()){
    drawRect(rarityColor(i.rarity),x+3,y+size-5,size-6,3);
   }itemIcon(item,x+size*.15f,y+size*.10f,size/31f);}
  }
  float invLeft(float vw){return vw/2-375;}
  float invTop(float vh){return vh/2-204;}
  void drawInventory(float vw,float vh){
   float left=invLeft(vw),top=invTop(vh);
   panel(left,top,750,402);drawRect(col("#c6a66c"),left+10,top+56,730,2);drawRect(col("#8c754d"),left+304,top+67,2,300);
   paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setColor(col("#f3d7a1"));paint.setTextSize(22);
   canvas.drawText("ABENTEURER  /  INVENTAR",left+18,top+30,paint);
   paint.setTextSize(11);paint.setColor(col("#9fc2c0"));canvas.drawText("Hotbar anklicken: Platz waehlen / leeren  |  Item anklicken: zuweisen",left+18,top+48,paint);
   panel(left+14,top+65,285,313);
   paint.setTextSize(14);paint.setColor(col("#f5d7a5"));canvas.drawText("CHARAKTER",left+28,top+87,paint);
   drawRect(col("#1b302e"),left+85,top+143,132,152);
   drawRect(col("#32564a"),left+89,top+147,124,144);
   drawSprite(sword?"player_sword":"player_unarmed","idle",0,left+151,top+217,125,143,elapsed,false);
   paint.setTextSize(12);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setColor(col("#f2deb6"));canvas.drawText("ABENTEURER  -  Lv. "+playerLevel,left+75,top+316,paint);paint.setTypeface(Typeface.DEFAULT);
   String[] slots={"Kopf","Waffe","Brust","Fuesse","Guertel"};
   float[][] positions={{left+129,top+97},{left+21,top+155},{left+222,top+155},{left+21,top+246},{left+222,top+246}};
   for(int i=0;i<slots.length;i++){
    float sx=positions[i][0],sy=positions[i][1];String name=equipped.get(slots[i]);
    slotFrame(sx,sy,43,name);
    paint.setColor(name==null?col("#b7c1bf"):rarityColor(definitions.get(name).rarity));
    paint.setTextSize(9);canvas.drawText(slots[i],sx,sy-5,paint);
   }
   drawRect(col("#121d24"),left+24,top+330,267,39);
   paint.setColor(col("#e2c78f"));paint.setTextSize(12);
   canvas.drawText("SCHADEN "+attackPower+"    RUESTUNG "+armorValue(),left+32,top+346,paint);
   paint.setColor(col("#91c8ed"));canvas.drawText("HP "+hp+"/"+maxHp+"    MANA "+mana+"/"+maxMana,left+32,top+362,paint);
   panel(left+315,top+65,419,313);
   paint.setColor(col("#f1d5a5"));paint.setTextSize(14);canvas.drawText("RUCKSACK",left+330,top+87,paint);
   String[] names=inventoryNames();
   for(int i=0;i<Math.min(24,names.length);i++){
    float x=left+329+(i%6)*67,y=top+104+(i/6)*64;
    slotFrame(x,y,56,names[i]);
    Item item=definitions.get(names[i]);paint.setColor(item!=null&&item.equipment()?rarityColor(item.rarity):col("#d5dad9"));
    paint.setTextSize(9);String label=names[i];if(label.length()>10)label=label.substring(0,9)+".";
    canvas.drawText(label,x+2,y+49,paint);
    paint.setColor(col("#ffe4a5"));paint.setTextSize(10);canvas.drawText(""+inventory.get(names[i]),x+42,y+12,paint);
   }
   if(hoverSlot>=0&&hoverSlot<names.length){
    Item item=definitions.get(names[hoverSlot]);
    if(item!=null&&item.equipment())drawGearTooltip(item,hoverX,hoverY,vw,vh);
   }
   paint.setTypeface(Typeface.DEFAULT);
  }
  void drawGearTooltip(Item item,float mx,float my,float vw,float vh){
   float tx=Math.max(4,Math.min(vw-240,mx+18)),ty=Math.max(4,Math.min(vh-150,my+12));
   drawRect(col("#10151d"),tx-3,ty-3,238,147);
   drawRect(rarityColor(item.rarity),tx,ty,232,141);
   drawRect(col("#26303a"),tx+3,ty+3,226,135);
   itemIcon(item.name,tx+9,ty+8,1.4f);
   paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setColor(rarityColor(item.rarity));paint.setTextSize(13);
   canvas.drawText(item.name,tx+47,ty+23,paint);
   paint.setTextSize(11);canvas.drawText(item.rarity,tx+47,ty+41,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setColor(col("#e8e9dc"));
   canvas.drawText("Slot: "+item.slot,tx+10,ty+63,paint);
   paint.setColor(col("#8ee4a0"));canvas.drawText("Schaden +"+item.dmg,tx+10,ty+83,paint);
   canvas.drawText("Verteidigung +"+item.armor,tx+10,ty+102,paint);
   canvas.drawText("Max. HP +"+item.hp+"     Mana +"+item.mana,tx+10,ty+121,paint);
  }
  void drawShop(float vw,float vh){
   float left=vw/2-365,top=vh/2-185;
   panel(left,top,730,370);drawRect(col("#c5a36a"),left+365,top+48,2,302);
   paint.setColor(col("#f6dbab"));paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(21);
   canvas.drawText("ORK-HAENDLER",left+17,top+30,paint);
   paint.setTextSize(12);canvas.drawText("E / I schliessen   |   Klick: Kaufen / Verkaufen",left+302,top+28,paint);
   String[] selling=inventoryNames();
   for(int side=0;side<2;side++){
    float sx=left+(side==0?14:374);
    panel(sx,top+49,340,306);
    paint.setTextSize(15);paint.setColor(col("#f1d7a4"));
    canvas.drawText(side==0?"ANGEBOT":"DEIN VERKAUF",sx+13,top+72,paint);
    String[] names=side==0?shopStock:selling;
    for(int i=0;i<Math.min(12,names.length);i++){
     float x=sx+10+(i%3)*106,y=top+87+(i/3)*65;
     drawRect(col("#17212a"),x,y,101,60);
     drawRect(col("#58666b"),x+2,y+2,97,56);
     drawRect(col("#28343c"),x+4,y+4,93,52);
     itemIcon(names[i],x+4,y+5,1.45f);
     Item item=definitions.get(names[i]);
     paint.setTextSize(10);paint.setColor(item!=null&&item.equipment()?rarityColor(item.rarity):col("#f1eee1"));
     String label=names[i];if(label.length()>13)label=label.substring(0,12)+".";
     canvas.drawText(label,x+38,y+20,paint);
     if(side==1){paint.setColor(col("#ffffff"));paint.setTextSize(11);canvas.drawText("x"+inventory.getOrDefault(names[i],0),x+7,y+17,paint);}
     paint.setColor(col("#eac582"));paint.setTextSize(11);
     canvas.drawText((side==0?price(names[i]):Math.max(1,price(names[i])/2))+" Kupfer",x+38,y+40,paint);
    }
   }
   paint.setTypeface(Typeface.DEFAULT);
  }
  int shopIndex(float x,float y,float vw,float vh,boolean buy){
   float left=vw/2-365,top=vh/2-185;
   float dx=x-left-(buy?24:384),dy=y-top-87;
   if(dx<0||dy<0||dx>=318||dy>=260)return -1;
   int col=(int)(dx/106),row=(int)(dy/65);
   if(dx%106>=101||dy%65>=60)return -1;
   return row*3+col;
  }
  void saveGame(){
   try{
    org.json.JSONObject root=new org.json.JSONObject();
    root.put("px",px);root.put("py",py);root.put("indoors",indoors);root.put("currentHouse",currentHouse);root.put("worldTime",worldTime);root.put("mana",mana);root.put("hp",hp);root.put("level",playerLevel);root.put("exp",exp);
    root.put("gold",gold);root.put("silver",silver);root.put("copper",copper);root.put("kills",kills);root.put("animalKills",animalKills);
    org.json.JSONObject bag=new org.json.JSONObject();for(String name:inventory.keySet())bag.put(name,inventory.get(name));root.put("inventory",bag);
    org.json.JSONObject gear=new org.json.JSONObject();for(String slot:equipped.keySet())gear.put(slot,equipped.get(slot));root.put("equipped",gear);
    org.json.JSONArray bar=new org.json.JSONArray();for(String item:hotbar)bar.put(item==null?org.json.JSONObject.NULL:item);root.put("hotbar",bar);
    org.json.JSONArray enemies=new org.json.JSONArray();for(Mob m:mobs){org.json.JSONObject o=new org.json.JSONObject();
     o.put("kind",m.kind);o.put("x",m.x);o.put("y",m.y);o.put("hp",m.hp);o.put("maxHp",m.maxHp);o.put("level",m.level);o.put("damage",m.damage);o.put("dead",m.dead);enemies.put(o);}root.put("mobs",enemies);
    org.json.JSONArray ground=new org.json.JSONArray();for(Drop d:drops){org.json.JSONObject o=new org.json.JSONObject();o.put("item",d.item);o.put("x",d.x);o.put("y",d.y);ground.put(o);}root.put("drops",ground);
    getContext().getSharedPreferences("pixel_save",MODE_PRIVATE).edit().putString("save",root.toString()).apply();
    pickupNotice="Spiel gespeichert";noticeTime=3f;
   }catch(Exception ex){pickupNotice="Speichern fehlgeschlagen";noticeTime=3f;}
  }
  void loadGame(){
   try{
    String json=getContext().getSharedPreferences("pixel_save",MODE_PRIVATE).getString("save",null);
    if(json==null){pickupNotice="Kein Spielstand";noticeTime=3f;return;}
    org.json.JSONObject root=new org.json.JSONObject(json);
    px=(float)root.getDouble("px");py=(float)root.getDouble("py");indoors=root.optBoolean("indoors",false);currentHouse=root.optInt("currentHouse",0);worldTime=(float)root.optDouble("worldTime",0);playerLevel=root.getInt("level");exp=root.getInt("exp");
    gold=root.getInt("gold");silver=root.getInt("silver");copper=root.getInt("copper");kills=root.optInt("kills");animalKills=root.optInt("animalKills");
    inventory.clear();org.json.JSONObject bag=root.getJSONObject("inventory");java.util.Iterator<String> keys=bag.keys();while(keys.hasNext()){String k=keys.next();inventory.put(k,bag.getInt(k));}
    equipped.clear();org.json.JSONObject gear=root.getJSONObject("equipped");keys=gear.keys();while(keys.hasNext()){String k=keys.next();equipped.put(k,gear.getString(k));}
    java.util.Arrays.fill(hotbar,null);selectedHotbar=-1;org.json.JSONArray bar=root.getJSONArray("hotbar");for(int i=0;i<Math.min(5,bar.length());i++)if(!bar.isNull(i))hotbar[i]=bar.getString(i);
    recalcStats();hp=Math.min(maxHp,root.getInt("hp"));mana=Math.min(maxMana,root.optInt("mana",maxMana));cleanupHotbar();
    mobs.clear();org.json.JSONArray enemies=root.getJSONArray("mobs");for(int i=0;i<enemies.length();i++){org.json.JSONObject o=enemies.getJSONObject(i);
     Mob m=new Mob(o.getString("kind"),(float)o.getDouble("x"),(float)o.getDouble("y"));m.hp=o.getInt("hp");m.maxHp=o.getInt("maxHp");m.level=o.getInt("level");m.damage=o.getInt("damage");m.dead=o.getBoolean("dead");mobs.add(m);}
    drops.clear();org.json.JSONArray ground=root.getJSONArray("drops");for(int i=0;i<ground.length();i++){org.json.JSONObject o=ground.getJSONObject(i);drops.add(new Drop(o.getString("item"),(float)o.getDouble("x"),(float)o.getDouble("y"),0));}
    startScreen=false;pauseMenu=false;inventoryOpen=false;shopOpen=false;joyX=joyY=0;previous=System.nanoTime();
    pickupNotice="Spielstand geladen";noticeTime=3f;
   }catch(Exception ex){pickupNotice="Laden fehlgeschlagen";noticeTime=3f;}
  }
  void drawMenu(float vw,float vh){
   drawRect(col("#101820"),0,0,vw,vh);
   float left=vw/2-250,top=vh/2-208;
   panel(left,top,500,416);drawRect(col("#c6a66c"),left+16,top+77,468,2);drawRect(col("#b38d54"),left+16,top+296,468,2);
   paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);
   paint.setColor(col("#f2cf8b"));paint.setTextSize(30);canvas.drawText("PIXEL ABENTEUER",vw/2,top+47,paint);
   paint.setColor(col("#b6c8c2"));paint.setTextSize(13);
   canvas.drawText(startScreen?"Willkommen in deiner Welt":"PAUSE  /  SPIELMENUE",vw/2,top+70,paint);
   String[] buttons={startScreen?"SPIEL STARTEN":"WEITERSPIELEN","SPIEL SPEICHERN","SPIEL LADEN","NEUSTART"};
   for(int i=0;i<buttons.length;i++){float y=top+89+i*49;drawRect(col("#7b7259"),left+95,y,310,42);
    drawRect(col("#334a4c"),left+98,y+3,304,36);paint.setColor(col("#f6e7c6"));paint.setTextSize(17);canvas.drawText(buttons[i],vw/2,y+26,paint);}
   paint.setColor(col("#e5cc9c"));paint.setTextSize(15);canvas.drawText("STEUERUNG",vw/2,top+311,paint);
   paint.setTextSize(12);paint.setColor(col("#d8e2d9"));
   canvas.drawText("WASD: Laufen   |   Linksklick: Angriff   |   E: Aufheben / Handel",vw/2,top+335,paint);
   canvas.drawText("I: Inventar   |   1-5: Hotbar   |   ESC: Pause",vw/2,top+355,paint);
   canvas.drawText("Ausrüstung: Im Inventar anklicken zum An- oder Ausziehen",vw/2,top+376,paint);
   paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.DEFAULT);
  }
  void menuClick(float x,float y,float vw,float vh){
   float top=vh/2-208,left=vw/2-250;
   if(x<left+95||x>left+405)return;
   for(int i=0;i<4;i++){float by=top+89+i*49;if(y>=by&&y<=by+42){
    if(i==0){startScreen=false;pauseMenu=false;previous=System.nanoTime();}
    if(i==1)saveGame();
    if(i==2)loadGame();
    if(i==3){restart();startScreen=false;pauseMenu=false;}
    return;
   }}
  }
  void drawHotbar(float vw,float vh){
   for(int i=0;i<5;i++){
    float x=vw/2-128+i*53,y=vh-53;
    drawRect(inventoryOpen&&selectedHotbar==i?col("#f0ca78"):col("#141c23"),x-2,y-2,50,48);drawRect(col("#4d5b62"),x,y,46,44);
    if(hotbar[i]!=null)itemIcon(hotbar[i],x+7,y+2,1.3f);
    paint.setColor(Color.WHITE);paint.setTextSize(12);canvas.drawText(""+(i+1),x+3,y+41,paint);
    if(hotbar[i]!=null){paint.setTextSize(11);canvas.drawText(""+inventory.getOrDefault(hotbar[i],0),x+32,y+41,paint);}
   }
  }
  void healthBar(float x,float y,float width,int current,int maximum){
   float ratio=Math.max(0,Math.min(1,current/(float)Math.max(1,maximum)));
   drawRect(col("#231f20"),x-width/2-2,y-2,width+4,9);
   drawRect(col("#7a2d2b"),x-width/2,y,width,5);
   drawRect(col("#60c86c"),x-width/2,y,width*ratio,5);
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
   paint.setColor(Color.WHITE);paint.setColorFilter(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,new Rect(frame*frameWidth,0,(frame+1)*frameWidth,b.getHeight()),new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);
  }
  void update(float dt){
   if(!startScreen&&!pauseMenu&&!inventoryOpen&&!shopOpen)worldTime+=dt;
   elapsed+=dt;noticeTime=Math.max(0,noticeTime-dt);swordTimer=Math.max(0,swordTimer-dt);sword=swordTimer>0;attackCooldown=Math.max(0,attackCooldown-dt);attackAnim=Math.max(0,attackAnim-dt);hurtAnim=Math.max(0,hurtAnim-dt);playerHealthVisible=Math.max(0,playerHealthVisible-dt);playerDamageTime=Math.max(0,playerDamageTime-dt);
   if(hp<=0){deathAnim+=dt;return;}if(inventoryOpen||shopOpen||pauseMenu||startScreen)return;
   float mx=joyX,my=joyY,length=(float)Math.hypot(mx,my);
   if(length>1){mx/=length;my/=length;}
   if(indoors){
    if(Math.abs(mx)+Math.abs(my)>.08f)playerDir=direction(mx,my);
    float nx=Math.max(105,Math.min(695,px+mx*140*dt));
    float ny=Math.max(205,Math.min(492,py+my*140*dt));
    // Partition occupies x=375..402 in interior player coordinates.
    // The middle opening (y=354..444) is deliberately passable.
    if(!((nx>=378&&nx<=401)&&(ny<389)))px=nx;
    py=ny;
    if(py>482&&Math.abs(px-DOOR_IN_X)<38&&my>0)enterOrLeave();
    return;
   }
   if(Math.abs(mx)+Math.abs(my)>.08f){
    playerDir=Math.abs(mx)>Math.abs(my)?(mx<0?2:3):(my<0?1:0);
    float step=140*dt/3f;
    for(int sub=0;sub<3;sub++){
     float nx=px+mx*step,ny=py+my*step;
     if(!blocked(nx,py))px=nx;
     if(!blocked(px,ny))py=ny;
     if(Math.abs(px-(forgeX+DOOR_OUT_X))<22&&py>forgeY+DOOR_OUT_Y-22&&py<forgeY+DOOR_OUT_Y+10&&my<0){enterOrLeave();break;}
    }
   }
   if(indoors)return;
   for(Mob m:mobs){
    if(m.dead){m.deathTimer+=dt;continue;}
    m.anim+=dt;m.attackTimer=Math.max(0,m.attackTimer-dt);m.hurtTimer=Math.max(0,m.hurtTimer-dt);m.healthVisible=Math.max(0,m.healthVisible-dt);m.damageNumberTime=Math.max(0,m.damageNumberTime-dt);
    float distance=(float)Math.hypot(px-m.x,py-m.y);
    float vx=0,vy=0;String action="idle";
    if(m.hurtTimer>0){action="hurt";}
    else if(m.hostile()&&distance<285&&!safeZone(m.x,m.y)&&!safeZone(px,py)){
     m.dir=direction(px-m.x,py-m.y);
     if(distance>43){vx=(px-m.x)/Math.max(1,distance)*75;vy=(py-m.y)/Math.max(1,distance)*75;action="run";}
     else {action="attack";if(m.attackTimer<=0){int dealt=Math.max(1,m.damage);hp=Math.max(0,hp-Math.max(1,dealt-armorValue()));lastPlayerDamage=dealt;playerDamageTime=.9f;hurtAnim=.32f;playerHealthVisible=3f;m.attackTimer=1.3f;}}
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
    if(safeZone(m.x,m.y)||safeZone(m.x+vx*dt,m.y+vy*dt)){vx=0;vy=0;m.vx=0;m.vy=0;}
    if(Math.abs(vx)+Math.abs(vy)>1){
     float step=dt/3f;
     for(int sub=0;sub<3;sub++){
      float nx=m.x+vx*step,ny=m.y+vy*step;
      if(!blocked(nx,m.y)&&(!m.hostile()||!safeZone(nx,m.y)))m.x=nx;else m.decision=0;
      if(!blocked(m.x,ny)&&(!m.hostile()||!safeZone(m.x,ny)))m.y=ny;else m.decision=0;
     }
     m.dir=direction(vx,vy);
    }
    if(!m.action.equals(action)){m.action=action;m.anim=0;}
   }
  }
  int direction(float vx,float vy){return Math.abs(vx)>Math.abs(vy)?(vx<0?2:3):(vy<0?1:0);}
  void attack(){
   if(hp<=0||attackCooldown>0||inventoryOpen||shopOpen||pauseMenu||startScreen)return;
   attackCooldown=.42f;attackAnim=.4f;swordTimer=3f;sword=true;
   float fx=playerDir==2?-1:playerDir==3?1:0,fy=playerDir==1?-1:playerDir==0?1:0;
   for(Mob m:mobs){
    if(m.dead)continue;
    float dx=m.x-px,dy=m.y-py,d=(float)Math.hypot(dx,dy);
    if(d>76||d<.01f)continue;
    if((dx*fx+dy*fy)/d<-.1f)continue;
    m.hp-=attackPower;m.lastDamage=attackPower;m.damageNumberTime=.9f;m.hurtTimer=.28f;m.healthVisible=3f;m.anim=0;
    if(m.hp<=0){m.dead=true;m.deathTimer=0;if(m.hostile())kills++;else animalKills++;earnExp((m.hostile()?8:3)+m.level*2);loot(m);}
   }
  }
  void restart(){
   indoors=false;currentHouse=0;playerLevel=1;exp=0;attackPower=3;maxHp=12;hp=maxHp;maxMana=20;mana=20;worldTime=0;equipped.clear();copper=silver=gold=0;java.util.Arrays.fill(hotbar,null);selectedHotbar=-1;playerDamageTime=0;px=180;py=180;joyX=joyY=0;deathAnim=0;swordTimer=0;sword=false;attackAnim=0;playerHealthVisible=0;kills=animalKills=0;mobs.clear();drops.clear();inventory.clear();inventoryOpen=false;shopOpen=false;pauseMenu=false;spawnMobs();
  }
  @Override protected void onDraw(Canvas c){
   canvas=c;float dt=Math.min(.05f,(System.nanoTime()-previous)/1000000000f);previous=System.nanoTime();update(dt);
   float w=getWidth(),h=getHeight(),scale=Math.min(w/800f,h/450f),vw=w/scale,vh=h/scale;
   paint.reset();paint.setAntiAlias(false);paint.setFilterBitmap(false);paint.setColor(Color.WHITE);
   paint.setColorFilter(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
   // Clear the entire frame, including any emulator-provided dirty region.
   c.drawColor(col("#47884a"),android.graphics.PorterDuff.Mode.SRC);c.save();c.scale(scale,scale);c.save();c.translate(vw/2-px,vh/2-py);
   int startX=(int)Math.floor((px-vw/2)/32f)*32-32,endX=(int)Math.ceil((px+vw/2)/32f)*32+32;
   int startY=(int)Math.floor((py-vh/2)/32f)*32-32,endY=(int)Math.ceil((py+vh/2)/32f)*32+32;
   for(int x=startX;x<endX;x+=32)for(int y=startY;y<endY;y+=32){
    int n=Math.floorMod(x/32*37+y/32*19,7);drawRect(n<3?col("#407d46"):col("#47884a"),x,y,32,32);
    if(n==1)drawRect(col("#6a9a53"),x+8,y+7,5,4);
   }
   for(Decoration d:decorations){
    if(Math.hypot(d.x-forgeX,d.y-(forgeY+84))<82)continue;
    if(d.image!=null){paint.setColor(Color.WHITE);paint.setColorFilter(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);c.drawBitmap(d.image,null,new RectF(d.x-d.w/2,d.y-d.h*.8f,d.x+d.w/2,d.y+d.h*.2f),paint);}
    else drawRect(col("#82776b"),d.x-d.w/2,d.y-d.h/2,d.w,d.h);
   }
   drawHouse(forgeX,forgeY,true);
   drawSprite("orc2","idle",0,merchantX,merchantY,79,79,elapsed,false);
   paint.setTextAlign(Paint.Align.CENTER);paint.setColor(col("#ffe7aa"));paint.setTextSize(13);
   canvas.drawText("HAENDLER",merchantX,merchantY-45,paint);paint.setTextAlign(Paint.Align.LEFT);
   if(Math.hypot(px-merchantX,py-merchantY)<100&&!shopOpen){paint.setColor(Color.WHITE);paint.setTextSize(12);
    canvas.drawText("E - Handeln",merchantX-37,merchantY-58,paint);}
   for(Drop d:drops){float bob=(float)Math.sin(elapsed*3+d.bob)*2;
    drawRect(col("#28342b"),d.x-11,d.y+7,22,5);itemIcon(d.item,d.x-10,d.y-15+bob,.82f);
   }
   // Day-night tint on terrain and decorations, behind creatures and UI.
   float phase=(worldTime%240f)/240f;
   float night=(float)Math.max(0,Math.min(1,(-Math.cos(phase*6.2831853f)-.10f)/.85f));
   paint.setColor(Color.argb((int)(night*70),13,21,52));paint.setStyle(Paint.Style.FILL);
   c.drawRect(px-vw/2-8,py-vh/2-8,px+vw/2+8,py+vh/2+8,paint);
   for(Mob m:mobs){
    if(m.dead&&m.deathTimer>2.5f)continue;
    String action=m.dead?"death":m.hurtTimer>0?"hurt":m.action;
    float size=m.hostile()?79:m.kind.equals("boar")?45:m.kind.equals("deer")?43:36;
    drawSprite(m.kind,action,m.dir,m.x,m.y,size,size,m.dead?m.deathTimer:m.anim,m.dead&&m.deathTimer>.7f);
    if(!m.dead){paint.setColor(Color.WHITE);paint.setTextSize(12);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("Lv. "+m.level,m.x,m.y-size*.64f,paint);paint.setTextAlign(Paint.Align.LEFT);}
    if(m.healthVisible>0&&(!m.dead||m.deathTimer<1f))healthBar(m.x,m.y-size*.65f-12,42,m.hp,m.maxHp);
    damageText(m.x,m.y-size*.65f-22,m.lastDamage,m.damageNumberTime);
   }
   String playerKind=sword?"player_sword":"player_unarmed";
   String playerAction=hp<=0?"death":hurtAnim>0?"hurt":attackAnim>0&&sword?"attack":Math.abs(joyX)+Math.abs(joyY)>.08f?"run":"idle";
   drawSprite(playerKind,playerAction,playerDir,px,py,94,94,hp<=0?deathAnim:attackAnim>0?.4f-attackAnim:elapsed,hp<=0&&deathAnim>.8f);
   if(playerHealthVisible>0)healthBar(px,py-56,52,hp,maxHp);
   damageText(px,py-66,lastPlayerDamage,playerDamageTime);
   Drop nearby=nearestDrop();
   if(nearby!=null&&!inventoryOpen){paint.setColor(col("#fff0c5"));paint.setTextSize(13);paint.setTextAlign(Paint.Align.CENTER);
    canvas.drawText("E  "+nearby.item+" aufheben",px,py-60,paint);paint.setTextAlign(Paint.Align.LEFT);}
   c.restore();
   if(indoors)drawInterior(vw,vh);else drawClouds(vw,vh);
   drawRect(col("#142b24"),0,0,vw,37);paint.setColor(Color.WHITE);paint.setTextSize(18);
   c.drawText("Lv. "+playerLevel+" EXP "+exp+"/"+expNeeded()+" DMG "+attackPower+" DEF "+armorValue(),12,25,paint);
   itemIcon("Golden Coin",vw-202,7,.85f);itemIcon("Silver Coin",vw-137,7,.85f);itemIcon("Copper Coin",vw-72,7,.85f);
   paint.setColor(Color.WHITE);paint.setTextSize(15);c.drawText(""+gold,vw-178,25,paint);c.drawText(""+silver,vw-113,25,paint);c.drawText(""+copper,vw-48,25,paint);
   drawHotbar(vw,vh);
   if(noticeTime>0){paint.setColor(col("#f7e0a6"));paint.setTextSize(17);c.drawText(pickupNotice,12,60,paint);}
   if(inventoryOpen)drawInventory(vw,vh);
   if(startScreen||pauseMenu)drawMenu(vw,vh);
   if(shopOpen)drawShop(vw,vh);
   if(hp<=0){drawRect(col("#111111"),vw/2-180,vh/2-45,360,90);paint.setColor(Color.WHITE);paint.setTextSize(24);c.drawText("GAME OVER - Tippen",vw/2-145,vh/2+7,paint);}
   c.restore();postInvalidateDelayed(16);
  }
  int inventoryIndex(float x,float y,float vw,float vh){
   float left=invLeft(vw),top=invTop(vh);
   float dx=x-left-329,dy=y-top-104;
   if(dx<0||dy<0||dx>=6*67||dy>=4*64)return -1;
   int col=(int)(dx/67),row=(int)(dy/64);
   if(dx%67>=56||dy%64>=56)return -1;
   return row*6+col;
  }
  void updateInventoryHover(MotionEvent e){
   if(!inventoryOpen)return;
   float sc=Math.min(getWidth()/800f,getHeight()/450f),vw=getWidth()/sc,vh=getHeight()/sc;
   hoverX=e.getX()/sc;hoverY=e.getY()/sc;
   hoverSlot=inventoryIndex(hoverX,hoverY,vw,vh);
   invalidate();
  }
  @Override public boolean onHoverEvent(MotionEvent e){
   if(inventoryOpen){updateInventoryHover(e);return true;}
   return super.onHoverEvent(e);
  }
  @Override public boolean onGenericMotionEvent(MotionEvent e){
   if(inventoryOpen){updateInventoryHover(e);return true;}
   if((e.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE&&e.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS
      &&e.getActionButton()==MotionEvent.BUTTON_PRIMARY){attack();return true;}
   return super.onGenericMotionEvent(e);
  }
  @Override public boolean onTouchEvent(MotionEvent e){
   int type=e.getActionMasked();
   if(inventoryOpen&&(type==MotionEvent.ACTION_MOVE||type==MotionEvent.ACTION_HOVER_MOVE)){updateInventoryHover(e);return true;}
   if(type==MotionEvent.ACTION_DOWN){
    if(startScreen||pauseMenu){float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;menuClick(xx,yy,vw,vh);return true;}
    if(shopOpen){
     float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;
     int idx=shopIndex(xx,yy,vw,vh,true);
     if(idx>=0&&idx<Math.min(12,shopStock.length))trade(shopStock[idx],true);
     else{idx=shopIndex(xx,yy,vw,vh,false);String[] names=inventoryNames();if(idx>=0&&idx<Math.min(12,names.length))trade(names[idx],false);}
     return true;
    }
    if(inventoryOpen){updateInventoryHover(e);float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;
     int barSlot=hotbarIndexAt(xx,yy,vw,vh);
     if(barSlot>=0){if(selectedHotbar==barSlot){hotbar[barSlot]=null;selectedHotbar=-1;}else selectedHotbar=barSlot;return true;}
     int equippedIndex=equippedSlotAt(xx,yy,vw,vh);if(equippedIndex>=0){unequip(new String[]{"Kopf","Waffe","Brust","Fuesse","Guertel"}[equippedIndex]);return true;}
     int idx=inventoryIndex(xx,yy,vw,vh);if(idx>=0){String[] names=inventoryNames();if(idx<names.length){String name=names[idx];Item item=definitions.get(name);if(item!=null){if(item.equipment())equip(name);else if(item.heal>0)assignHotbar(name);}}}return true;}
    if(hp<=0){restart();return true;}
    // In BlueStacks, a left mouse click arrives as a primary mouse/touch press.
    if((e.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE){
     if(e.getButtonState()==0||(e.getButtonState()&MotionEvent.BUTTON_PRIMARY)!=0)attack();
    }else attack();
   }
   return true;
  }
  @Override public boolean onKeyDown(int key,KeyEvent e){
   if(key==KeyEvent.KEYCODE_ESCAPE||key==KeyEvent.KEYCODE_BACK){
    if(startScreen)return true;
    if(shopOpen)shopOpen=false;else if(inventoryOpen)inventoryOpen=false;else pauseMenu=!pauseMenu;
    joyX=joyY=0;return true;
   }
   if(startScreen||pauseMenu){if(key==KeyEvent.KEYCODE_ENTER){startScreen=false;pauseMenu=false;}return true;}
   switch(key){case KeyEvent.KEYCODE_A:case KeyEvent.KEYCODE_DPAD_LEFT:joyX=-1;break;
    case KeyEvent.KEYCODE_D:case KeyEvent.KEYCODE_DPAD_RIGHT:joyX=1;break;
    case KeyEvent.KEYCODE_W:case KeyEvent.KEYCODE_DPAD_UP:joyY=-1;break;
    case KeyEvent.KEYCODE_S:case KeyEvent.KEYCODE_DPAD_DOWN:joyY=1;break;
    case KeyEvent.KEYCODE_SPACE:attack();break;
    case KeyEvent.KEYCODE_E:if(shopOpen)shopOpen=false;else if(indoors||Math.hypot(px-forgeX,py-(forgeY+84))<65)enterOrLeave();else if(Math.hypot(px-merchantX,py-merchantY)<100)openShop();else pickup();break;
    case KeyEvent.KEYCODE_I:if(shopOpen)shopOpen=false;else inventoryOpen=!inventoryOpen;hoverSlot=-1;break;
    case KeyEvent.KEYCODE_1:useHotbar(0);break;
    case KeyEvent.KEYCODE_2:useHotbar(1);break;
    case KeyEvent.KEYCODE_3:useHotbar(2);break;
    case KeyEvent.KEYCODE_4:useHotbar(3);break;
    case KeyEvent.KEYCODE_5:useHotbar(4);break;
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
