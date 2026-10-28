package de.pixelabenteuer.game;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.media.SoundPool;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
  gameView=new Game();setContentView(gameView);
 }
 @Override protected void onPause(){super.onPause();if(gameView!=null)gameView.pauseMusic();}
 @Override protected void onResume(){super.onResume();if(gameView!=null)gameView.resumeMusic();}
 @Override protected void onDestroy(){if(gameView!=null)gameView.releaseMusic();super.onDestroy();}
 Game gameView;
 class Mob {
  String kind,action="idle";float x,y,vx,vy,anim=0,decision=0,attackTimer=0,hurtTimer=0,deathTimer=0,healthVisible=0;int hp,maxHp,level,damage,dir=0;boolean dead=false,alpha=false;Mob leader=null;float pathTimer=0;int pathIndex=0;java.util.ArrayList<float[]> path=new java.util.ArrayList<>();float damageNumberTime=0;int lastDamage=0;
  Mob(String k,float xx,float yy) {kind=k;x=xx;y=yy;level=1+(int)(Math.random()*(k.startsWith("orc")?6:4));int baseHp=k.equals("orc3")?12:k.equals("orc2")?9:k.equals("orc1")?7:k.equals("boar")?8:k.equals("deer")?5:k.equals("fox")?4:2;maxHp=baseHp+(level-1)*(k.equals("boar")||k.startsWith("orc")?3:2);hp=maxHp;damage=(k.equals("orc3")?3:k.equals("orc2")?2:k.equals("orc1")?1:0)+(level-1)/3;}
  boolean hostile(){return kind.startsWith("orc")||kind.equals("boar");}
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
  int nextGearId=1;
  String uniqueGear(String template){
   Item base=definitions.get(template);if(base==null||!base.equipment())return template;
   String id=template+" {"+(nextGearId++)+"}";
   Item item=new Item(id,base.rarity,base.slot,base.dmg,base.armor,base.hp,base.heal);item.mana=base.mana;
   int rank=base.rarity.equals("Legendär")?4:base.rarity.equals("Super Rare")?3:base.rarity.equals("Rare")?2:base.rarity.equals("Uncommon")?1:0;
   int[] strength={0,2,5,8,12},count={0,1,1,2,3};
   for(int k=0;k<count[rank];k++){int bonus=strength[rank]+random.nextInt(Math.max(1,strength[rank]/2+1));int stat=random.nextInt(3);
    if(stat==0)item.hp+=bonus*2;else if(stat==1&&item.slot.equals("Waffe"))item.dmg+=bonus;else if(stat==1)item.armor+=bonus;else item.mana+=bonus;
   }
   definitions.put(id,item);return id;
  }
  String[] hotbar=new String[9];int selectedHotbar=-1;int copper=0,silver=0,gold=0,hoverSlot=-1;float hoverX=0,hoverY=0;
  Item inspectedGear=null;String characterName="Abenteurer";int armorStyle=0,characterGender=0;boolean sprintHeld=false;int attackSequence=0;boolean creatorOpen=false,calendarOpen=false;int hairType=0,hairColor=1,skinColor=1,eyeColor=0,combatStyle=0;boolean inventoryOpen=false,shopOpen=false,startScreen=true,pauseMenu=false,controlsOpen=false;float merchantX=265,merchantY=215;
  int activeMerchant=0; // 0 Orc-Händler, 1 Schmied, 2 Händlerin
  String[] shopStock={"Apple","Ham","Healthpotion","Healpotion med","Iron Sword","Silver Sword","Leather Armor","Iron Armor","Iron Helmet","Golden Sword"};
  String[] smithStock={"Wooden Sword [Common]","Wooden Sword [Legendär]","Iron Sword [Uncommon]","Iron Sword [Rare]","Silver Sword [Common]","Silver Sword [Legendär]","Golden Sword [Rare]","Golden Sword [Legendär]","Leather Armor [Rare]","Iron Armor [Rare]","Iron Helmet [Legendär]","Leather Boot [Uncommon]"};
  final float STOCK_REFRESH_SECONDS=300f;
  float stockRefreshTimer=0f;
  int stockRotation=0;
  String[] sellerStock={"Apple","Green Apple","Ham","Cheese","Mushroom","Healthpotion","Healthpotion large","Healpotion med"};
  String pickupNotice="";float noticeTime=0;
  HashMap<String,Bitmap> sprites=new HashMap<>();HashMap<String,Bitmap> characterSheets=new HashMap<>();
  float px=180,py=180,joyX=0,joyY=0,elapsed=0,worldTime=0,attackAnim=0,hurtAnim=0,deathAnim=0,attackCooldown=0,playerHealthVisible=0;
  int hp=12,maxHp=12,kills=0,animalKills=0,playerDir=0,playerLevel=1,exp=0,attackPower=3,lastPlayerDamage=0;float playerDamageTime=0;float swordTimer=0;boolean sword=false;
  long previous=System.nanoTime();
  AudioTrack[] musicTracks=new AudioTrack[3];
  SoundPool hitSounds;
  int enemyHitSound=0,playerHitSound=0;
  float[] musicVolumes=new float[3];
  float masterMusicVolume=0.05f;
  void setMusicVolume(float value){masterMusicVolume=Math.max(0f,Math.min(1f,value));getContext().getSharedPreferences("pixel_settings",MODE_PRIVATE).edit().putFloat("music_volume",masterMusicVolume).apply();}
  boolean musicPaused=false;
  // Three original looping 8-bit melodies, generated locally without external audio assets.
  short[] makeMelody(int theme){
   final int rate=22050,beat=rate/4,bars=64;
   short[] pcm=new short[beat*bars];
   // Exploration: bright pentatonic flute and arpeggiated harp.
   // Combat: fast minor riff, distorted bass and percussion.
   // Shop: relaxed waltz with bell-like chords.
   int[][] tunes={
    {60,64,67,72,67,64,62,67,60,64,69,72,74,72,69,64,65,69,72,76,72,69,67,64,62,65,69,74,72,69,67,64,
     60,67,64,72,69,64,62,67,65,69,72,74,76,72,69,67,64,67,72,76,74,72,69,65,62,65,69,72,67,64,62,60},
    {40,40,43,46,40,40,48,46,38,38,41,45,38,45,48,41,40,43,46,48,46,43,40,38,36,36,43,46,48,46,43,41,
     40,40,52,51,40,43,46,48,38,38,50,48,38,41,45,48,36,43,46,48,50,48,46,43,40,40,43,46,48,46,43,40},
    {64,67,71,67,62,66,69,66,60,64,67,64,62,65,69,65,64,67,72,67,62,66,69,66,60,64,67,72,71,67,64,60,
     64,67,71,76,74,71,67,64,62,66,69,74,72,69,66,62,60,64,67,72,71,67,64,60,62,65,69,74,72,69,65,64}
   };
   for(int i=0;i<pcm.length;i++){
    int step=i/beat,within=i%beat;
    double t=i/(double)rate,local=within/(double)rate;
    double note=tunes[theme][step],hz=440*Math.pow(2,(note-69)/12.0);
    double fade=Math.min(1,within/320.0)*Math.min(1,(beat-within)/1200.0);
    double signal;
    if(theme==0){
     double flute=Math.sin(2*Math.PI*hz*local)+.18*Math.sin(4*Math.PI*hz*local);
     double harp=Math.sin(2*Math.PI*(hz*2)*local)*Math.exp(-local*12);
     double low=Math.sin(2*Math.PI*(hz/2)*t)*.18;
     signal=(flute*.42+harp*.25+low)*fade;
    }else if(theme==1){
     double lead=Math.tanh(3*Math.sin(2*Math.PI*hz*local));
     double bass=Math.tanh(2*Math.sin(2*Math.PI*(hz/2)*t));
     double kick=Math.sin(2*Math.PI*70*local)*Math.exp(-local*32);
     double snare=((Math.sin(i*2.17)+Math.sin(i*1.31))*.5)*Math.exp(-local*28);
     signal=(lead*.38*fade+bass*.30+kick*.26+snare*(step%2==1?.16:.06));
    }else{
     double bell=(Math.sin(2*Math.PI*hz*local)+.5*Math.sin(2*Math.PI*hz*2*local)+.2*Math.sin(2*Math.PI*hz*3*local))*Math.exp(-local*7);
     double chordHz=440*Math.pow(2,((step/4%3==0?48:step/4%3==1?53:55)-69)/12.0);
     double chord=Math.sin(2*Math.PI*chordHz*t)*.23+Math.sin(2*Math.PI*chordHz*1.5*t)*.12;
     signal=bell*.53+chord*.6;
    }
    pcm[i]=(short)(Math.max(-1,Math.min(1,signal))*23500);
   }
   return pcm;
  }
  // Stream the generated PCM in a background writer: static AudioTrack buffers
  // are not reliably supported at this length on all Android devices.
  volatile boolean musicRunning=false;
  Thread[] musicWriters=new Thread[3];
  byte[] hitWav(boolean player){
   int rate=22050,count=player?4200:2800;
   byte[] out=new byte[44+count*2];
   java.nio.ByteBuffer buf=java.nio.ByteBuffer.wrap(out).order(java.nio.ByteOrder.LITTLE_ENDIAN);
   buf.put(new byte[]{'R','I','F','F'});buf.putInt(36+count*2);
   buf.put(new byte[]{'W','A','V','E','f','m','t',' '});buf.putInt(16);
   buf.putShort((short)1);buf.putShort((short)1);buf.putInt(rate);
   buf.putInt(rate*2);buf.putShort((short)2);buf.putShort((short)16);
   buf.put(new byte[]{'d','a','t','a'});buf.putInt(count*2);
   for(int i=0;i<count;i++){
    double t=i/(double)rate,env=Math.exp(-t*(player?19:29));
    double hz=player?165-95*t:440-310*t;
    double noise=Math.sin(i*2.39)*Math.sin(i*.83);
    double sound=player?(Math.sin(2*Math.PI*hz*t)*.6+noise*.35):(Math.sin(2*Math.PI*hz*t)*.65+noise*.28);
    buf.putShort((short)(Math.max(-1,Math.min(1,sound*env))*24000));
   }
   return out;
  }
  void initHitSounds(){
   try{
    hitSounds=new SoundPool.Builder().setMaxStreams(5)
      .setAudioAttributes(new android.media.AudioAttributes.Builder()
       .setUsage(android.media.AudioAttributes.USAGE_GAME)
       .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
    for(int i=0;i<2;i++){
     byte[] wav=hitWav(i==1);
     File file=new File(getContext().getCacheDir(),"pixel_hit_"+i+".wav");
     try(FileOutputStream stream=new FileOutputStream(file)){stream.write(wav);}
     android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
     int id=hitSounds.load(fd.getFileDescriptor(),0,wav.length,1);fd.close();
     if(i==0)enemyHitSound=id;else playerHitSound=id;
    }
   }catch(Exception e){android.util.Log.e("PixelSound","Hit sounds unavailable",e);}
  }
  void playHit(boolean player){
   if(hitSounds!=null&&!musicPaused)hitSounds.play(player?playerHitSound:enemyHitSound,.9f,.9f,1,0,1f);
  }
  void startMusic(){
   if(musicRunning)return;
   musicRunning=true;
   for(int i=0;i<3;i++){
    final int index=i;
    try{
     final short[] data=makeMelody(i);
     int buffer=Math.max(8192,AudioTrack.getMinBufferSize(22050,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT));
     final AudioTrack track=new AudioTrack(AudioManager.STREAM_MUSIC,22050,AudioFormat.CHANNEL_OUT_MONO,
       AudioFormat.ENCODING_PCM_16BIT,buffer,AudioTrack.MODE_STREAM);
     if(track.getState()!=AudioTrack.STATE_INITIALIZED){track.release();continue;}
     musicTracks[i]=track;
     track.setStereoVolume(0f,0f);
     track.play();
     musicWriters[i]=new Thread(()->{
      int offset=0;
      while(musicRunning){
       try{
        int count=Math.min(2048,data.length-offset);
        int written=track.write(data,offset,count);
        if(written<0)break;
        if(written==0){Thread.yield();continue;}
        offset=(offset+written)%data.length;
       }catch(Exception e){break;}
      }
     },"PixelMusic-"+index);
     musicWriters[i].setDaemon(true);
     musicWriters[i].start();
    }catch(Exception e){android.util.Log.e("PixelMusic","Cannot start track "+i,e);}
   }
  }
  void pauseMusic(){musicPaused=true;for(AudioTrack track:musicTracks)if(track!=null)try{track.pause();}catch(Exception ignored){}}
  void resumeMusic(){musicPaused=false;for(AudioTrack track:musicTracks)if(track!=null)try{track.play();}catch(Exception ignored){}}
  void releaseMusic(){
   if(hitSounds!=null){hitSounds.release();hitSounds=null;}
   musicRunning=false;
   for(int i=0;i<musicTracks.length;i++)if(musicTracks[i]!=null){
    try{musicTracks[i].pause();musicTracks[i].flush();musicTracks[i].release();}catch(Exception ignored){}
    musicTracks[i]=null;
   }
  }
  void updateMusic(float dt){
   int target=0;
   if(indoors)target=2;
   else if(!safeZone(px,py))for(Mob m:mobs)
    if(!m.dead&&m.hostile()&&!safeZone(m.x,m.y)&&Math.hypot(px-m.x,py-m.y)<285){target=1;break;}
   for(int i=0;i<3;i++){
    float goal=i==target?.85f:0f;
    float step=dt/.9f*.85f;
    musicVolumes[i]+=Math.max(-step,Math.min(step,goal-musicVolumes[i]));
    if(musicTracks[i]!=null&&!musicPaused)try{
     musicTracks[i].setStereoVolume(musicVolumes[i]*masterMusicVolume,musicVolumes[i]*masterMusicVolume);
    }catch(Exception ignored){}
   }
  }
  Game(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);paint.setFilterBitmap(false);loadSprites();loadBuildings();loadClouds();loadItems();loadDecorations();spawnMobs();masterMusicVolume=getContext().getSharedPreferences("pixel_settings",MODE_PRIVATE).getFloat("music_volume",0.05f);startMusic();initHitSounds();}
  void spawnMobs(){
   String[] types={"boar","deer","orc1","orc2","orc3","fox","black_grouse"};
   int animals=0,orcs=0;
   for(Mob m:mobs)if(!m.dead){if(m.kind.startsWith("orc"))orcs++;else animals++;}
   for(int attempts=0;attempts<180&&(animals<28||orcs<18);attempts++){
    String kind=types[random.nextInt(types.length)];
    boolean orc=kind.startsWith("orc");
    if((orc&&orcs>=18)||(!orc&&animals>=28))continue;
    float x=orc?380+random.nextInt(1100):80+random.nextInt(1440),y=80+random.nextInt(840);
    if(blocked(x,y)||safeZone(x,y)||Math.hypot(x-merchantX,y-merchantY)<110)continue;
    int available=(orc?18-orcs:28-animals);
    int groupSize=(kind.equals("boar")||kind.equals("deer")||orc)?Math.min(available,3+random.nextInt(2)):1;
    Mob alpha=new Mob(kind,x,y);
    if((orc||kind.equals("boar"))&&groupSize>1){alpha.alpha=true;alpha.maxHp=Math.round(alpha.maxHp*1.7f);alpha.hp=alpha.maxHp;alpha.damage+=2;}
    mobs.add(alpha);if(orc)orcs++;else animals++;
    for(int n=1;n<groupSize;n++){
     float angle=(float)(n*6.2831853f/groupSize),radius=28+random.nextInt(15);
     float gx=x+(float)Math.cos(angle)*radius,gy=y+(float)Math.sin(angle)*radius;
     if(blocked(gx,gy)||safeZone(gx,gy))continue;
     Mob follower=new Mob(kind,gx,gy);follower.leader=alpha;mobs.add(follower);
     if(orc)orcs++;else animals++;
    }
   }
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
   for(int i=0;i<5;i++){
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
    float overlapOpacity=overPlayer?.06f:1f; // 80% more transparent on contact.
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
  final HashMap<String,Rect> npcCropCache=new HashMap<>();
  void characterTile(String name,int columns,int rows,int frame,float x,float y,float size){
   Bitmap bitmap=buildingSprites.get(name);if(bitmap==null)return;
   int fw=bitmap.getWidth()/columns,fh=bitmap.getHeight()/rows;
   int index=Math.floorMod(frame,columns*rows),sx=(index%columns)*fw,sy=(index/columns)*fh;
   // Trim transparent padding inside the selected animation cell so all NPCs
   // have the same visible height as the player, regardless of sheet spacing.
   String cacheKey=name+":"+columns+":"+rows+":"+index;
   Rect crop=npcCropCache.get(cacheKey);
   if(crop==null){
    int minX=fw,minY=fh,maxX=-1,maxY=-1;
    for(int yy=0;yy<fh;yy++)for(int xx=0;xx<fw;xx++){
     if(Color.alpha(bitmap.getPixel(sx+xx,sy+yy))>32){
      if(xx<minX)minX=xx;if(xx>maxX)maxX=xx;
      if(yy<minY)minY=yy;if(yy>maxY)maxY=yy;
     }
    }
    crop=new Rect(minX,minY,maxX+1,maxY+1);
    npcCropCache.put(cacheKey,crop);
   }
   if(crop.width()<=0||crop.height()<=0)return;
   float h=size*.78f,w=h*crop.width()/(float)crop.height();
   w=Math.min(w,size*.85f);
   tile(name,sx+crop.left,sy+crop.top,crop.width(),crop.height(),x-w/2,y-h,w,h);
  }
  void sellerPortrait(float x,float y,int frame){
   Bitmap bitmap=buildingSprites.get("Seller");if(bitmap==null)return;
   // Seller.png is 96x96: each of the 3x2 cells is 32x48.
   // Its 48px-high cells contain an unwanted second row fragment.
   // Isolate the first standing character within the upper 32x32 area.
   int sx=Math.floorMod(frame,3)*32,sy=0,sw=Math.min(32,bitmap.getWidth()-sx),sh=Math.min(32,bitmap.getHeight());
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
   paint.setColor(col("#f5d795"));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(10);
   paint.setTextSize(20);fantasyName("Schmiede",x,y-150);paint.setTextAlign(Paint.Align.LEFT);
  }
  // Door anchors are shared by the automatic movement triggers and teleportation.
  final float DOOR_OUT_X=0f,DOOR_OUT_Y=102f;
  final float DOOR_IN_X=555f,DOOR_IN_Y=518f;
  void enterOrLeave(){
   if(indoors){
    indoors=false;px=forgeX+DOOR_OUT_X;py=forgeY+DOOR_OUT_Y+43f;
    joyX=joyY=0;return;
   }
   if(Math.abs(px-(forgeX+DOOR_OUT_X))<22&&
      py>forgeY+DOOR_OUT_Y-22&&py<forgeY+DOOR_OUT_Y+10){
    indoors=true;currentHouse=1;px=DOOR_IN_X;py=DOOR_IN_Y-31f;
    joyX=joyY=0;
   }
  }
  // The player's Tiled map is read once, preserving the exact CSV layer order.
  long[][] smithyLayers;
  boolean smithyMapTried=false;
  void loadSmithyMap(){
   if(smithyMapTried)return;
   smithyMapTried=true;
   // Exact tile IDs exported from the user's nine-layer Tiled map.
   // Embedded to avoid Android asset XML parser/packaging incompatibilities.
   String[] rows={
    "382:1,429:21,295:20,382:1,445:21,311:20,382:1,461:21,327:19,295:1,382:1,429:21,343:19,311:1,382:1,445:21,359:19,327:1,382:1,461:21,375:19,343:1,382:1,429:21,295:19,359:1,382:1,445:21,311:19,375:1,382:1,461:21,327:19,295:1,382:1,429:21,343:19,311:1,382:1,445:21,359:19,327:1,382:1,461:21,375:19,343:1,382:1,429:21,295:19,359:1,382:1,445:21,311:19,375:1,382:1,461:21,327:19,295:1,382:1,429:21,343:19,295:1,382:1,445:21,295:19,311:1,382:1,461:21,311:19,327:1,382:1,429:21,327:19,343:1,382:1,429:1,445:6,429:14,343:19,359:1,382:1,445:1,429:6,445:14,359:19,375:1,408:41,400:1",
    "0:870,392:2,0:52",
    "0:275,970:1,971:1,972:1,971:14,973:1,0:24,983:1,984:16,986:1,0:24,983:1,984:8,985:3,984:1,985:4,986:1,0:24,983:1,985:13,984:3,986:1,0:24,983:1,985:1,984:14,985:1,986:1,0:24,996:1,998:1,997:5,998:4,997:5,998:1,999:1,0:421",
    "0:118,966:1,967:1,968:1,969:1,0:38,979:1,980:1,981:1,982:1,0:38,992:1,993:1,994:1,995:1,0:193,680:1,681:1,686:1,687:1,688:1,689:1,656:1,657:1,658:1,685:1,686:1,687:1,688:1,689:1,685:1,686:1,661:1,662:1,663:1,690:1,664:1,0:21,693:1,694:1,699:1,700:1,701:1,702:1,669:1,670:1,671:1,698:1,699:1,700:1,701:1,702:1,698:1,699:1,674:1,675:1,676:1,703:1,677:1,0:65,970:1,972:1,971:1,972:1,971:13,973:1,0:24,983:1,985:5,984:8,985:1,984:2,986:1,0:24,983:1,985:1,984:1,985:3,984:1,985:1,984:2,985:1,984:3,985:3,986:1,0:4,966:1,967:1,968:1,969:1,0:16,983:1,985:1,984:2,985:2,984:3,985:1,984:1,985:2,984:2,985:2,986:1,0:4,979:1,980:1,981:1,982:1,0:16,983:1,984:1,985:1,984:2,985:3,984:2,985:1,984:1,985:1,984:2,985:2,986:1,0:4,992:1,993:1,994:1,995:1,0:16,983:1,985:1,984:4,985:2,984:2,985:1,984:1,985:1,984:2,985:2,986:1,0:24,983:1,984:1,985:1,984:5,985:4,984:1,985:1,984:1,985:2,986:1,0:24,996:1,998:16,999:1,0:85",
    "5:1,0:41,5:1,0:41,5:1,0:41,176:1,0:41,176:1,0:41,176:1,0:41,176:1,0:41,176:1,0:8,966:1,967:1,968:1,969:1,0:29,176:1,0:8,979:1,980:1,981:1,982:1,0:29,176:1,0:8,992:1,993:1,994:1,995:1,0:29,176:1,0:41,176:1,0:41,176:1,914:1,915:1,916:1,917:1,0:37,176:1,927:1,928:1,929:1,930:1,0:37,176:1,940:1,941:1,942:1,943:1,0:21,1009:1,1010:1,0:14,176:1,953:1,954:1,955:1,956:1,0:19,1009:1,1010:1,1022:1,1023:1,0:14,176:1,0:23,1022:1,1023:1,0:16,176:1,0:41,176:1,0:167",
    "304:1,59:3,230:2,59:1,230:3,59:1,116:1,59:1,230:6,59:1,61:1,176:1,319:1,415:1,416:1,292:1,291:1,419:1,425:1,292:1,291:1,420:1,421:1,292:1,291:1,476:1,477:1,292:1,291:1,425:1,426:1,290:1,304:1,59:2,116:1,59:1,230:6,59:1,230:1,59:1,230:1,59:1,230:1,59:1,116:1,59:1,175:1,176:1,335:1,431:1,432:1,308:1,307:1,435:1,441:1,308:1,307:1,436:1,437:1,308:1,307:1,492:1,493:1,308:1,307:1,441:1,442:1,306:1,304:1,192:19,175:1,176:1,351:1,447:1,448:1,324:1,323:1,451:1,457:1,324:1,323:1,452:1,453:1,324:1,323:1,508:1,509:1,324:1,323:1,457:1,458:1,322:1,304:1,0:19,118:1,176:1,0:3,340:1,339:1,0:2,340:1,339:1,0:2,340:1,339:1,0:2,340:1,339:1,0:2,302:1,304:1,0:19,175:1,176:1,0:19,302:1,304:1,0:19,175:1,119:1,0:19,302:1,304:1,0:19,175:1,176:1,0:19,302:1,304:1,0:19,118:1,176:1,0:19,302:1,304:1,0:19,175:1,176:1,0:19,302:1,304:1,0:19,80:1,81:1,0:19,302:1,304:1,0:18,354:1,327:1,343:1,0:18,708:1,302:1,304:1,0:17,1610613090:1,327:1,311:1,343:1,0:13,709:1,710:1,711:1,712:1,713:1,714:1,302:1,304:1,0:17,1610613090:1,327:2,343:1,0:13,722:1,723:1,724:1,725:1,726:1,727:1,302:1,304:1,0:18,369:1,327:1,343:1,0:2,1067:1,1068:1,1111:1,1112:1,0:7,735:1,736:1,737:1,738:1,739:1,740:1,302:1,304:1,0:19,3221225553:1,3221225552:1,0:2,1080:1,1081:1,1124:1,1125:1,0:7,748:1,749:1,750:1,751:1,752:1,753:1,302:1,304:1,0:19,175:1,176:1,0:1,709:1,710:1,711:1,712:1,713:1,714:1,708:1,0:5,709:1,710:1,711:1,706:1,707:1,708:1,302:1,304:1,0:19,175:1,176:1,0:1,722:1,723:1,724:1,725:1,726:1,727:1,708:1,0:5,722:1,723:1,724:1,719:1,720:1,721:1,302:1,304:1,0:19,175:1,119:1,0:1,735:1,736:1,737:1,738:1,739:1,740:1,0:6,735:1,736:1,737:1,732:1,733:1,734:1,302:1,304:1,0:19,63:1,176:1,0:1,748:1,749:1,750:1,751:1,752:1,753:1,0:6,748:1,749:1,750:1,745:1,746:1,747:1,302:1,415:1,416:1,417:1,418:1,419:1,424:1,425:1,423:1,424:1,425:1,423:1,424:1,425:1,423:1,319:1,425:1,423:1,424:1,425:1,423:1,424:1,427:1,415:1,416:1,417:1,418:1,419:1,423:1,424:1,425:1,396:1,397:1,423:1,424:1,425:1,319:1,424:1,425:1,423:1,425:1,426:1,427:1,431:1,432:1,433:1,434:1,435:1,440:1,441:1,439:1,440:1,441:1,439:1,440:1,441:1,439:1,335:1,441:1,439:1,440:1,441:1,439:1,440:1,443:1,431:1,432:1,433:1,434:1,435:1,439:1,440:1,441:1,396:1,397:1,439:1,440:1,441:1,335:1,440:1,441:1,439:1,441:1,442:1,443:1,447:1,448:1,449:1,450:1,451:1,456:1,457:1,455:1,456:1,457:1,455:1,456:1,457:1,455:1,351:1,457:1,455:1,456:1,457:1,455:1,456:1,459:1,447:1,448:1,449:1,450:1,451:1,455:1,456:1,457:1,412:1,413:1,455:1,456:1,457:1,351:1,456:1,457:1,455:1,457:1,458:1,459:1",
    "0:9,921:1,922:1,923:1,924:1,0:14,817:1,818:1,819:1,0:21,934:1,935:1,936:1,937:1,0:14,830:1,831:1,832:1,0:1,1039:1,1040:1,0:11,1111:1,1112:1,0:5,947:1,948:1,949:1,950:1,0:4,873:1,874:1,0:3,918:1,919:1,920:1,0:2,843:1,844:1,845:1,0:1,1052:1,1053:1,1069:1,0:10,1124:1,1125:1,0:5,960:1,961:1,962:1,963:1,0:4,886:1,887:1,0:3,931:1,932:1,933:1,0:2,856:1,857:1,858:1,0:1,1065:1,1066:1,1082:1,0:25,899:1,900:1,0:3,944:1,945:1,946:1,0:62,1005:1,1006:1,0:40,1018:1,1019:1,0:271,1154:1,0:32,918:1,919:1,920:1,0:26,912:1,913:1,0:11,931:1,932:1,933:1,0:26,925:1,926:1,0:9,1011:1,1012:1,944:1,945:1,946:1,0:26,938:1,939:1,0:9,1024:1,1025:1,957:1,958:1,959:1,0:192",
    "0:5,558:1,559:1,560:1,561:1,0:4,558:1,559:1,560:1,561:1,0:22,951:1,952:1,0:6,566:1,567:1,568:1,569:1,0:4,566:1,567:1,568:1,569:1,0:22,964:1,965:1,0:6,574:1,575:1,576:1,577:1,0:4,574:1,575:1,576:1,577:1,0:9,990:1,991:1,0:6,1087:1,1088:1,1089:1,1090:1,0:1,977:1,978:1,0:6,582:1,583:1,584:1,585:1,990:1,991:1,990:1,991:1,582:1,583:1,584:1,585:1,0:9,1003:1,1004:1,0:6,1100:1,1101:1,1102:1,1103:1,0:9,590:1,591:1,592:1,593:1,1003:1,1004:1,1003:1,1004:1,590:1,591:1,592:1,593:1,0:7,762:1,763:1,764:1,0:5,762:1,763:1,764:1,0:2,768:1,769:1,770:1,0:7,598:1,599:1,600:1,601:1,0:4,598:1,599:1,600:1,601:1,0:1,1005:1,1006:1,0:4,775:1,776:1,777:1,0:5,775:1,776:1,777:1,0:2,781:1,782:1,783:1,0:16,820:1,821:1,822:1,0:1,1018:1,1019:1,0:4,788:1,789:1,790:1,0:5,788:1,789:1,790:1,0:2,794:1,795:1,796:1,0:16,833:1,834:1,835:1,0:7,801:1,802:1,803:1,0:5,801:1,802:1,803:1,0:2,807:1,808:1,809:1,0:16,846:1,847:1,848:1,0:39,859:1,860:1,861:1,0:4,680:1,0:41,693:1,0:247,1005:1,1006:1,0:36,1005:1,1006:1,0:2,1018:1,1019:1,0:36,1018:1,1019:1,0:153",
    "0:70,1152:1,0:37,1111:1,1112:1,0:2,1141:1,0:11,1041:1,1042:1,0:24,1124:1,1125:1,0:13,666:1,1054:1,1055:1,0:1,286:1,287:1,288:1,0:6,1005:1,1006:1,0:5,286:1,287:1,288:1,0:10,866:1,867:1,868:1,0:6,679:1,0:3,302:1,303:1,304:1,0:6,1018:1,1019:1,0:5,302:1,303:1,304:1,0:10,879:1,880:1,881:1,0:10,318:1,319:1,320:1,0:2,974:1,975:1,976:1,0:7,666:1,318:1,319:1,320:1,0:10,892:1,893:1,894:1,0:10,334:1,335:1,336:1,0:2,987:1,988:1,989:1,0:7,679:1,334:1,335:1,336:1,0:10,905:1,906:1,907:1,0:10,350:1,351:1,352:1,0:2,1000:1,1001:1,1002:1,0:8,350:1,351:1,352:1,1104:1,1105:1,0:40,1117:1,1118:1,0:40,1130:1,1131:1,0:82,1104:1,1105:1,0:16,1144:1,0:23,1117:1,1118:1,0:16,1158:1,0:23,1130:1,0:89,1145:1,0:17,1041:1,1042:1,1078:1,1079:1,990:1,991:1,0:7,990:1,991:1,1011:1,1012:1,0:7,1152:1,0:17,1054:1,1055:1,1091:1,1092:1,1003:1,1004:1,0:7,1003:1,1004:1,1024:1,1025:1,0:149"
   };
   smithyLayers=new long[rows.length][42*22];
   try{
    for(int n=0;n<rows.length;n++){
     int index=0;
     for(String pair:rows[n].split(",")){
      String[] parts=pair.split(":");
      long id=Long.parseLong(parts[0]);
      int count=Integer.parseInt(parts[1]);
      for(int j=0;j<count;j++)smithyLayers[n][index++]=id;
     }
     if(index!=42*22)throw new IllegalStateException("Bad smithy layer "+n);
    }
   }catch(Exception e){android.util.Log.e("PixelAbenteuer","Cannot decode smithy map",e);smithyLayers=null;}
  }
  void drawSmithyMap(float l,float t){
   loadSmithyMap();
   if(smithyLayers==null)return;
   String[] atlas={"Decorative_cracks","Walls_interior","Forge","Interior_objects"};
   int[] first={1,286,558,654},cols={19,16,8,13},counts={285,272,96,507};
   paint.setColor(Color.WHITE);paint.setColorFilter(null);paint.setFilterBitmap(false);paint.setAlpha(255);
   for(long[] layer:smithyLayers)for(int i=0;i<layer.length;i++){
    long encoded=layer[i],gid=encoded&0x1fffffffL;
    if(gid==0)continue;
    int set=-1;
    for(int k=0;k<4;k++)if(gid>=first[k]&&gid<first[k]+counts[k]){set=k;break;}
    if(set<0)continue;
    Bitmap bitmap=buildingSprites.get(atlas[set]);if(bitmap==null)continue;
    int local=(int)gid-first[set],sx=(local%cols[set])*16,sy=(local/cols[set])*16;
    if(sx+16>bitmap.getWidth()||sy+16>bitmap.getHeight())continue;
    float x=l+(i%42)*16,y=t+(i/42)*16;
    boolean flipH=(encoded&0x80000000L)!=0,flipV=(encoded&0x40000000L)!=0,diag=(encoded&0x20000000L)!=0;
    if(flipH||flipV||diag){
     canvas.save();canvas.translate(x+8,y+8);
     if(diag){canvas.rotate(90);canvas.scale(1,-1);}
     canvas.scale(flipH?-1:1,flipV?-1:1);
     canvas.drawBitmap(bitmap,new Rect(sx,sy,sx+16,sy+16),new RectF(-8,-8,8,8),paint);
     canvas.restore();
    }else canvas.drawBitmap(bitmap,new Rect(sx,sy,sx+16,sy+16),new RectF(x,y,x+16,y+16),paint);
   }
  }
  void drawInterior(float vw,float vh){
   float l=vw/2-340,t=vh/2-176;
   drawRect(col("#18141a"),0,0,vw,vh);
   // Render the exact nine user-designed Tiled layers instead of the old hardcoded interior.
   drawSmithyMap(l,t);
   // The seller is drawn only once, using a single stable pose.
   characterTile("Master_Idle",3,2,(int)(elapsed*2)%3,l+177,t+145,81.4f);
   sellerPortrait(l+510,t+161,(int)(elapsed*3)%3);
   paint.setTextAlign(Paint.Align.CENTER);
   paint.setTypeface(Typeface.DEFAULT_BOLD);
   paint.setTextSize(13);paint.setColor(col("#ffe7aa"));
   fantasyName("Schmied",l+177,t+77);
   fantasyName("Händlerin",l+510,t+114);
   paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(11);
   paint.setColor(col("#ffffff"));
   if(Math.hypot(px-237,py-337)<53)drawInteractionKey(l+177,t+104);
   if(Math.abs(px-570)<58&&py>344&&py<409)drawInteractionKey(l+510,t+134);
   paint.setTextAlign(Paint.Align.LEFT);
   characterTile("Customer",3,2,(int)(elapsed*3)%3,l+562,t+288,52);
   paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT_BOLD);
   paint.setColor(col("#ffe7aa"));paint.setTextSize(13);
   fantasyName("Abenteurer",l+562,t+241);
   if(questAvailable()){
    paint.setColor(col("#50ef78"));paint.setTextSize(25);
    outlinedText("?",l+562,t+235);
    paint.setColor(col("#ffe7aa"));paint.setTextSize(13);
   }
   if(Math.hypot(px-622,py-462)<37.5f){
    paint.setTextSize(11);paint.setColor(Color.WHITE);
    drawInteractionKey(l+562,t+250);
   }
   if(adventurerSpeechTime>0&&!adventurerSpeech.isEmpty()){
    int alpha=(int)(255*Math.min(1f,adventurerSpeechTime/.85f));
    paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT_BOLD);
    paint.setTextSize(12);paint.setColor(Color.argb(alpha,255,246,214));
    String[] words=adventurerSpeech.split(" ");
    String line="",second="";
    for(String word:words){
     if(second.isEmpty()&&(line+" "+word).trim().length()<=23)line=(line+" "+word).trim();
     else second=(second+" "+word).trim();
    }
    canvas.drawText(line,l+562,t+205,paint);
    if(!second.isEmpty())canvas.drawText(second,l+562,t+219,paint);
   }
   paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.DEFAULT);
   float cx=l+340+(px-400),cy=t+176+(py-350);
   String indoorAction=hp<=0?"death":hurtAnim>0?"hurt":attackAnim>0&&sword?"attack":Math.abs(joyX)+Math.abs(joyY)>.08f?(sprintHeld?"run":"walk"):"idle";
   drawSprite(sword?"player_sword":"player_unarmed",indoorAction,playerDir,cx,cy,80.64f,80.64f,hp<=0?deathAnim:attackAnim>0?.4f-attackAnim:elapsed,hp<=0&&deathAnim>.8f);
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
   java.util.Random terrainRandom=new java.util.Random(91827L);
   // Reject intersecting decoration rectangles; keep house, entrance and spawn clear.
   for(int i=0,tries=0;i<4&&tries<200;tries++){
    float x=100+terrainRandom.nextInt(1400),y=80+terrainRandom.nextInt(820);
    if(!decorationSpaceFree(x,y,94,90,230f))continue;
    Bitmap b=ruins.isEmpty()?null:ruins.get(i%ruins.size());
    decorations.add(new Decoration(b,b,x,y,94,90,true));i++;
   }
   for(int i=0,tries=0;i<85&&tries<1600;tries++){
    float x=60+terrainRandom.nextInt(1480),y=65+terrainRandom.nextInt(870);
    float w=20+(i%3)*4,h=19+(i%4)*4;
    if(!decorationSpaceFree(x,y,w,h,210f))continue;
    Bitmap b=rocks.isEmpty()?null:rocks.get(i%rocks.size());
    decorations.add(new Decoration(b,rockMasks.isEmpty()?b:rockMasks.get(i%rockMasks.size()),x,y,w,h,true));i++;
   }
   for(int i=0,tries=0;i<70&&!plants.isEmpty()&&tries<1200;tries++){
    float x=30+terrainRandom.nextInt(1540),y=40+terrainRandom.nextInt(920);
    float w=34+(i%3)*12,h=40+(i%3)*10;
    if(!decorationSpaceFree(x,y,w,h,210f))continue;
    Bitmap b=plants.get(i%plants.size());
    decorations.add(new Decoration(b,b,x,y,w,h,true));i++;
   }
  }
  boolean decorationSpaceFree(float x,float y,float w,float h,float houseRadius){
   if(Math.hypot(x-forgeX,y-forgeY)<houseRadius)return false;
   if(Math.hypot(x-180f,y-180f)<145f)return false;
   if(Math.hypot(x-merchantX,y-merchantY)<85f)return false;
   float left=x-w/2f,right=x+w/2f,top=y-h*.8f,bottom=top+h;
   for(Decoration d:decorations){
    float dl=d.x-d.w/2f,dt=d.y-d.h*.8f;
    if(left<dl+d.w+12&&right>dl-12&&top<dt+d.h+12&&bottom>dt-12)return false;
   }
   return true;
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
  boolean spriteFootCollision(String name,int sx,int sy,int sw,int sh,float x,float y,float left,float top,float w,float h,float radius){
   Bitmap bitmap=buildingSprites.get(name);
   if(bitmap==null)return false;
   // Five probes approximate the player's feet, not the entire sprite body.
   float[] ox={0f,-radius,radius,0f,0f},oy={0f,0f,0f,-radius,radius};
   for(int i=0;i<ox.length;i++){
    float u=(x+ox[i]-left)/w,v=(y+oy[i]-top)/h;
    if(u<0||u>=1||v<0||v>=1)continue;
    int bx=sx+Math.min(sw-1,(int)(u*sw)),by=sy+Math.min(sh-1,(int)(v*sh));
    if(bx>=0&&by>=0&&bx<bitmap.getWidth()&&by<bitmap.getHeight()&&Color.alpha(bitmap.getPixel(bx,by))>48)return true;
   }
   return false;
  }
  boolean blocked(float x,float y){
   // Orc merchant: solid body, but still reachable using E from nearby.
   if(Math.hypot(x-merchantX,y-merchantY)<25f)return true;
   if(x<18||y<18||x>WORLD_W-18||y>WORLD_H-18)return true;
   for(Decoration d:decorations)if(d.solid&&Math.abs(d.x-x)<d.w*.5f+12f&&Math.abs(d.y-y)<d.h+14f&&Math.abs(d.x-forgeX)+Math.abs(d.y-forgeY)>262f&&collides(d,x,y))return true;
   float dx=x-forgeX,dy=y-forgeY;
   // Match the rendered house and props against their actual sprite alpha.
   // Sample a small foot area to avoid walking through visible edges.
   if(spriteFootCollision("Exterior_house",0,0,145,163,dx,dy,-116,-140,232,261,7f)
      // Leave the entrance passable for the existing door transition.
      &&!(Math.abs(dx-DOOR_OUT_X)<24&&dy>78))return true;
   if(spriteFootCollision("Exterior_house",70,162,32,38,dx,dy,-117,113,50,52,7f))return true;
   if(spriteFootCollision("Exterior_house",116,160,34,34,dx,dy,97,66,55,55,7f))return true;
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
   define("Wooden Shield","Common","Schild",0,1,0,0);
   define("Iron Shield","Rare","Schild",0,4,0,0);
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
   defineEquipmentVariants();
   refreshMerchantStocks();
  }
  String baseEquipmentName(String name){
   int unique=name.lastIndexOf(" {");if(unique>=0&&name.endsWith("}"))name=name.substring(0,unique);
   int pos=name.lastIndexOf(" [");
   return pos>=0&&name.endsWith("]")?name.substring(0,pos):name;
  }
  void defineEquipmentVariants(){
   String[] bases={"Wooden Sword","Iron Sword","Silver Sword","Golden Sword","Wooden Armor","Leather Armor","Iron Armor","Leather Helmet","Iron Helmet","Leather Boot","Iron Boot"};
   String[] rarities={"Common","Uncommon","Rare","Super Rare","Legendär"};
   for(String base:bases){
    Item original=definitions.get(base);
    if(original==null)continue;
    int tier=base.startsWith("Wooden")?0:base.startsWith("Leather")?1:base.startsWith("Iron")?2:base.startsWith("Silver")?3:4;
    for(int rank=0;rank<rarities.length;rank++){
     // Material tier dominates rarity; legendary wood remains weaker than silver.
     int power=tier*12+rank*2+1;
     int dmg=original.dmg>0?power:0;
     int armor=original.armor>0?Math.max(1,power/3):0;
     int bonusHp=original.hp>0?Math.max(1,power/2):0;
     define(base+" ["+rarities[rank]+"]",rarities[rank],original.slot,dmg,armor,bonusHp,0);
    }
   }
  }
  void itemIcon(String item,float x,float y,float scale){
   Bitmap b=itemImages.get(baseEquipmentName(item));
   if(b==null)return;
   paint.setColor(Color.WHITE);paint.setColorFilter(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,null,new RectF(x,y,x+24*scale,y+24*scale),paint);
  }
  int price(String name){
   Item i=definitions.get(name);
   if(i==null)return 3;
   if(i.equipment()){
    int rank=i.rarity.equals("Legendär")?4:i.rarity.equals("Super Rare")?3:i.rarity.equals("Rare")?2:i.rarity.equals("Uncommon")?1:0;
    String base=baseEquipmentName(name);
    int material=base.startsWith("Wooden")?1:base.startsWith("Leather")?2:base.startsWith("Iron")?3:base.startsWith("Silver")?4:base.startsWith("Golden")?5:2;
    Item baseline=definitions.get(name.replaceAll(" \\{[0-9]+\\}$",""));
    int bonus=baseline==null?0:Math.max(0,i.dmg-baseline.dmg)*18+Math.max(0,i.armor-baseline.armor)*16+Math.max(0,i.hp-baseline.hp)*8+Math.max(0,i.mana-baseline.mana)*8;
    return (20+material*55)*(rank+1)*(rank+1)+bonus;
   }
   return i.heal>0?Math.max(6,i.heal*4):5;
  }
  int wealth(){return gold*10000+silver*100+copper;}
  void setWealth(int value){value=Math.max(0,value);gold=value/10000;silver=(value/100)%100;copper=value%100;}
  void trade(String name,boolean buy){
   int cost=price(name);
   if(buy){if(!inventory.containsKey(name)&&inventory.size()>=24){pickupNotice="Inventar voll (24 Plaetze)";noticeTime=2;return;}if(wealth()<cost){pickupNotice="Nicht genug Muenzen";noticeTime=2;return;}
    setWealth(wealth()-cost);inventory.put(name,inventory.getOrDefault(name,0)+1);
    pickupNotice=name+" gekauft";
   }else{if(inventory.getOrDefault(name,0)<1)return;
    int count=inventory.get(name)-1;if(count<=0)inventory.remove(name);else inventory.put(name,count);
    setWealth(wealth()+Math.max(1,cost/2));cleanupHotbar();pickupNotice=name+" verkauft";
   }
   noticeTime=2;
  }
  void refreshMerchantStocks(){
   ArrayList<String> smithPool=new ArrayList<>();
   for(String name:new java.util.ArrayList<>(definitions.keySet())){
    if(name.matches(".* \\{[0-9]+\\}$"))continue;
    Item item=definitions.get(name);
    if(item!=null&&item.equipment()&&(name.startsWith("Wooden ")||name.startsWith("Leather ")||name.startsWith("Iron ")||name.startsWith("Silver ")||name.startsWith("Golden ")))smithPool.add(name);
   }
   Collections.sort(smithPool);
   Collections.shuffle(smithPool,random);
   smithStock=new String[Math.min(12,smithPool.size())];for(int i=0;i<smithStock.length;i++)smithStock[i]=uniqueGear(smithPool.get(i));
   ArrayList<String> orcPool=new ArrayList<>();
   // Keep the Orc merchant's mixed food, potions, weapons and armor assortment.
   String[] orcBasics={"Apple","Green Apple","Ham","Cheese","Mushroom","Healthpotion","Healthpotion large","Healpotion med"};
   Collections.addAll(orcPool,orcBasics);
   Collections.shuffle(orcPool,random);
   ArrayList<String> orcGear=new ArrayList<>(smithPool);
   Collections.shuffle(orcGear,random);
   ArrayList<String> offer=new ArrayList<>();
   for(int i=0;i<4&&i<orcPool.size();i++)offer.add(orcPool.get(i));
   for(int i=0;i<6&&i<orcGear.size();i++)offer.add(uniqueGear(orcGear.get(i)));
   shopStock=offer.toArray(new String[0]);
   stockRotation++;
  }
  String[] activeStock(){return activeMerchant==1?smithStock:activeMerchant==2?sellerStock:shopStock;}
  String merchantTitle(){return activeMerchant==1?"SCHMIED":activeMerchant==2?"HAENDLERIN":"ORC-HAENDLER";}
  void showMerchant(int merchant){activeMerchant=merchant;shopOpen=true;inventoryOpen=false;hoverSlot=-1;joyX=joyY=0;}
  String adventurerSpeech="";float adventurerSpeechTime=0f;
  // One active contract at a time. New contracts appear 2 or 3 game days after turn-in.
  int questTier=0,questProgress=0,nextQuestDay=0;
  boolean questActive=false;
  int questGoal(){return 3+questTier*2;}
  int currentQuestDay(){return (int)(worldTime/240f);}
  boolean questAvailable(){return !questActive&&currentQuestDay()>=nextQuestDay;}
  void talkAdventurer(){
   if(questActive&&questProgress>=questGoal()){
    int reward=25+questTier*15;
    copper+=reward;silver+=copper/100;copper%=100;gold+=silver/100;silver%=100;
    earnExp(12+questTier*5);
    questTier++;questActive=false;questProgress=0;
    nextQuestDay=currentQuestDay()+2+random.nextInt(2);
    adventurerSpeech="Stark! +"+reward+" Kupfer. Bis bald!";
   }else if(questAvailable()){
    questActive=true;questProgress=0;
    adventurerSpeech="Auftrag: Besiege "+questGoal()+" Orks!";
   }else if(questActive){
    adventurerSpeech="Orks besiegt: "+questProgress+"/"+questGoal()+". Weiter so!";
   }else{
    String[] lines={
     "Bleib wachsam da draussen!","Gute Beute gibt's nicht geschenkt.",
     "Mein Schwert braucht eine Pause.","Hast du schon den Wald erkundet?",
     "Ein Trank kann dein Leben retten.","Die Orks werden jede Nacht dreister.",
     "Ein guter Helm rettet Leben.","Ich habe schon bessere Klingen gesehen.",
     "Pass auf die Wildschweine auf!","Die Ruinen bergen alte Geheimnisse.",
     "Heute ist es ungewoehnlich ruhig.","Meine Stiefel sind voellig durchgelaufen.",
     "Der Schmied kennt sein Handwerk.","Hast du genug Proviant dabei?",
     "Ein mutiger Held kennt auch den Rueckzug.","Im Wald raschelt es nachts seltsam.",
     "Ich suche noch nach einer alten Karte.","Gold ist nicht alles, aber es hilft.",
     "Der Haendler feilscht wie ein Fuchs.","Manche Orks tragen seltene Beute.",
     "Eine scharfe Klinge ist die halbe Miete.","Die besten Geschichten beginnen am Lagerfeuer.",
     "Ich brauche noch etwas Ruhe.","Nicht jede Ruine ist verlassen."
    };
    adventurerSpeech=lines[random.nextInt(lines.length)];
   }
   adventurerSpeechTime=4f;
  }
  boolean openInteriorMerchant(){
   if(!indoors)return false;
   if(Math.hypot(px-622,py-462)<37.5f){talkAdventurer();return true;}
   if(Math.hypot(px-237,py-337)<53){showMerchant(1);return true;}
   if(Math.abs(px-570)<58&&py>344&&py<409){showMerchant(2);return true;}
   return false;
  }
  void openShop(){if(Math.hypot(px-merchantX,py-merchantY)<50)showMerchant(0);}
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
   final float step=39f,start=vw/2-hotbar.length*step/2f;
   float dx=x-start,dy=y-(vh-43);
   if(dx< -2||dy< -2||dy>37||dx>=hotbar.length*step)return -1;
   int slot=Math.max(0,(int)(Math.max(0,dx)/step));
   return slot<hotbar.length&&dx-slot*step<37?slot:-1;
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
     addDrop(uniqueGear(weapons[chance<.58f?0:chance<.85f?1:chance<.97f?2:3]),m.x,m.y);
    }
    if(random.nextFloat()<.28f){
     String[] armor={"Wooden Armor","Leather Armor","Iron Armor","Leather Helmet","Iron Helmet","Leather Boot","Iron Boot","Wizard Hat","Belt","Helm","Bag"};
     addDrop(uniqueGear(armor[random.nextInt(armor.length)]),m.x,m.y);
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
   if(!inventory.containsKey(d.item)&&inventory.size()>=24){pickupNotice="Inventar voll (24 Plaetze)";noticeTime=2;return;}
   inventory.put(d.item,inventory.containsKey(d.item)?inventory.get(d.item)+1:1);
   pickupNotice="+1 "+d.item;noticeTime=1.8f;drops.remove(d);
  }
  String[] inventoryNames(){return inventory.keySet().toArray(new String[0]);}
  void panel(float x,float y,float w,float h){
   drawRect(col("#101719"),x-5,y-5,w+10,h+10);
   drawRect(col("#a88751"),x-3,y-3,w+6,h+6);
   drawRect(col("#203638"),x,y,w,h);
   drawRect(col("#36534d"),x+4,y+4,w-8,3);
   drawRect(col("#765c38"),x+8,y+9,w-16,1);
   drawRect(col("#765c38"),x+8,y+h-10,w-16,1);
   for(int i=0;i<4;i++){
    float cx=x+(i%2==0?11:w-11),cy=y+(i<2?13:h-13);
    paint.setColor(col("#e1b86d"));canvas.drawCircle(cx,cy,3,paint);
    paint.setColor(col("#493c2a"));canvas.drawCircle(cx,cy,1,paint);
   }
  }
  void fantasyHeading(String value,float x,float y,int size){
   paint.setTypeface(Typeface.create("serif",Typeface.BOLD_ITALIC));
   paint.setTextSize(size);paint.setColor(col("#f6d89a"));
   canvas.drawText(value,x,y,paint);
   paint.setTypeface(Typeface.DEFAULT);
  }
  void slotFrame(float x,float y,float size,String item){
   drawRect(col("#171d20"),x-2,y-2,size+4,size+4);
   drawRect(col("#a08354"),x,y,size,size);
   drawRect(col("#263d3b"),x+3,y+3,size-6,size-6);
   if(item!=null){Item i=definitions.get(item);if(i!=null&&i.equipment()){
    drawRect(rarityColor(i.rarity),x+3,y+size-5,size-6,3);
   }itemIcon(item,x+size*.15f,y+size*.10f,size/31f);}
  }
  float invLeft(float vw){return vw/2-375;}
  float invTop(float vh){return vh/2-204;}
  void drawInventory(float vw,float vh){
   float left=invLeft(vw),top=invTop(vh);
   panel(left,top,750,402);drawRect(col("#c6a66c"),left+10,top+56,730,2);drawRect(col("#8c754d"),left+304,top+67,2,300);
   fantasyHeading("Abenteurer  /  Inventar",left+18,top+30,23);
   paint.setTextSize(11);paint.setColor(col("#9fc2c0"));canvas.drawText("Hotbar anklicken: Platz waehlen / leeren  |  Item anklicken: zuweisen",left+18,top+48,paint);
   panel(left+14,top+65,285,313);
   fantasyHeading("Charakter",left+28,top+87,16);
   drawRect(col("#1b302e"),left+85,top+143,132,152);
   drawRect(col("#32564a"),left+89,top+147,124,144);
   drawSprite(sword?"player_sword":"player_unarmed","idle",0,left+151,top+217,150,165,elapsed,false);
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
   fantasyHeading("Rucksack",left+330,top+87,16);
   String[] names=inventoryNames();
   for(int i=0;i<Math.min(24,names.length);i++){
    float x=left+329+(i%6)*67,y=top+104+(i/6)*64;
    slotFrame(x,y,56,names[i]);
    Item item=definitions.get(names[i]);paint.setColor(item!=null&&item.equipment()?rarityColor(item.rarity):col("#d5dad9"));
    paint.setTextSize(9);String label=names[i].replaceAll(" \\{[0-9]+\\}$","");
    while(label.length()>1&&paint.measureText(label)>52f)label=label.substring(0,label.length()-1);
    if(!label.equals(names[i].replaceAll(" \\{[0-9]+\\}$","")))label=label.substring(0,Math.max(1,label.length()-1))+"…";
    canvas.drawText(label,x+2,y+49,paint);
    paint.setColor(col("#ffe4a5"));paint.setTextSize(10);canvas.drawText(""+inventory.get(names[i]),x+42,y+12,paint);
   }
   if(inspectedGear!=null&&gearTooltipTime>0f)drawGearTooltip(inspectedGear,hoverX,hoverY,vw,vh);
   paint.setTypeface(Typeface.DEFAULT);
  }
  float gearTooltipTime=0f;
  void showGearTooltip(float xx,float yy,float vw,float vh){
   Item item=null;
   int equippedIndex=equippedSlotAt(xx,yy,vw,vh);
   if(equippedIndex>=0){String slot=new String[]{"Kopf","Waffe","Brust","Fuesse","Guertel"}[equippedIndex];item=definitions.get(equipped.get(slot));}
   else{int idx=inventoryIndex(xx,yy,vw,vh);String[] names=inventoryNames();if(idx>=0&&idx<names.length)item=definitions.get(names[idx]);}
   if(item!=null&&item.equipment()){inspectedGear=item;gearTooltipTime=8f;hoverX=xx;hoverY=yy;invalidate();}
  }
  void drawGearTooltip(Item item,float mx,float my,float vw,float vh){
   paint.setTextAlign(Paint.Align.LEFT);
   float tx=Math.max(4,Math.min(vw-240,mx+18)),ty=Math.max(4,Math.min(vh-150,my+12));
   drawRect(col("#10151d"),tx-3,ty-3,238,147);
   drawRect(rarityColor(item.rarity),tx,ty,232,141);
   drawRect(col("#26303a"),tx+3,ty+3,226,135);
   itemIcon(item.name,tx+9,ty+8,1.4f);
   paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setColor(rarityColor(item.rarity));paint.setTextSize(13);
   canvas.drawText(item.name.replaceAll(" \\{[0-9]+\\}$",""),tx+47,ty+23,paint);
   paint.setTextSize(11);canvas.drawText(item.rarity,tx+47,ty+41,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setColor(col("#e8e9dc"));
   canvas.drawText("Slot: "+item.slot,tx+10,ty+63,paint);
   paint.setColor(col("#8ee4a0"));float statY=ty+83;
   if(item.dmg>0){canvas.drawText("Schaden +"+item.dmg,tx+10,statY,paint);statY+=19;}
   if(item.armor>0){canvas.drawText("Verteidigung +"+item.armor,tx+10,statY,paint);statY+=19;}
   if(item.hp>0){canvas.drawText("Max. HP +"+item.hp,tx+10,statY,paint);statY+=19;}
   if(item.mana>0)canvas.drawText("Mana +"+item.mana,tx+10,statY,paint);
  }
  void showShopTooltip(float x,float y,float vw,float vh){int idx=shopIndex(x,y,vw,vh,true);String[] names=activeStock();if(idx<0){idx=shopIndex(x,y,vw,vh,false);names=inventoryNames();if(idx>=0)idx+=sellPage*12;}if(idx>=0&&idx<names.length){Item item=definitions.get(names[idx]);if(item!=null&&item.equipment()){inspectedGear=item;gearTooltipTime=8f;hoverX=x;hoverY=y;invalidate();}}}
  int sellPage=0;
  void drawShop(float vw,float vh){
   float left=vw/2-365,top=vh/2-185;
   panel(left,top,730,370);drawRect(col("#c5a36a"),left+365,top+48,2,302);
   fantasyHeading(merchantTitle(),left+17,top+30,23);
   if(activeMerchant!=2){
    paint.setTextSize(10);paint.setColor(col("#9fc2c0"));
    canvas.drawText("Neues Sortiment in "+Math.max(1,(int)Math.ceil((STOCK_REFRESH_SECONDS-stockRefreshTimer)/60f))+" Min.",left+17,top+44,paint);
    paint.setTextSize(21);paint.setColor(col("#f6dbab"));
   }
   paint.setTextSize(12);canvas.drawText("E / I schliessen   |   Klick: Kaufen / Verkaufen",left+302,top+28,paint);
   String[] selling=inventoryNames();
   int sellPages=Math.max(1,(selling.length+11)/12);sellPage=Math.min(sellPage,sellPages-1);
   paint.setTextSize(12);paint.setColor(col("#f6dbab"));
   canvas.drawText("<",left+596,top+72,paint);canvas.drawText((sellPage+1)+"/"+sellPages,left+615,top+72,paint);canvas.drawText(">",left+681,top+72,paint);
   for(int side=0;side<2;side++){
    float sx=left+(side==0?14:374);
    panel(sx,top+49,340,306);
    fantasyHeading(side==0?"Angebot":"Dein Verkauf",sx+13,top+72,16);
    String[] names=side==0?activeStock():selling;
    for(int i=0;i<Math.min(12,side==0?names.length:names.length-sellPage*12);i++){
     int itemIndex=side==0?i:sellPage*12+i;
     float x=sx+10+(i%3)*106,y=top+87+(i/3)*65;
     drawRect(col("#151d20"),x,y,101,60);
     drawRect(col("#9b7c4c"),x+2,y+2,97,56);
     drawRect(col("#253c3a"),x+4,y+4,93,52);
     itemIcon(names[itemIndex],x+4,y+5,1.45f);
     Item item=definitions.get(names[itemIndex]);
     paint.setTextSize(10);paint.setColor(item!=null&&item.equipment()?rarityColor(item.rarity):col("#f1eee1"));
     String label=names[itemIndex].replaceAll(" \\{[0-9]+\\}$","");
     paint.setTextSize(9);
     String line1=label,line2="";
     if(paint.measureText(line1)>58f){
      int cut=line1.length();while(cut>1&&paint.measureText(line1.substring(0,cut))>58f)cut--;
      int space=line1.lastIndexOf(' ',cut);
      if(space>2){line1=line1.substring(0,space);line2=label.substring(space+1);}else{line1=label.substring(0,cut);line2=label.substring(cut).trim();}
      while(line2.length()>1&&paint.measureText(line2)>58f)line2=line2.substring(0,line2.length()-1);
      if(!line2.isEmpty()&&!label.endsWith(line2))line2=line2.substring(0,Math.max(1,line2.length()-1))+"…";
     }
     canvas.drawText(line1,x+38,y+16,paint);
     if(!line2.isEmpty())canvas.drawText(line2,x+38,y+27,paint);
     if(side==1){paint.setColor(col("#ffffff"));paint.setTextSize(11);canvas.drawText("x"+inventory.getOrDefault(names[itemIndex],0),x+7,y+17,paint);}
     paint.setColor(col("#eac582"));paint.setTextSize(11);
     canvas.drawText((side==0?price(names[itemIndex]):Math.max(1,price(names[itemIndex])/2))+" Kupfer",x+38,y+45,paint);
    }
   }
   if(inspectedGear!=null&&gearTooltipTime>0f)drawGearTooltip(inspectedGear,hoverX,hoverY,vw,vh);
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
    root.put("characterName",characterName);root.put("armorStyle",armorStyle);root.put("characterGender",characterGender);root.put("hairType",hairType);root.put("hairColor",hairColor);root.put("skinColor",skinColor);root.put("eyeColor",eyeColor);root.put("combatStyle",combatStyle);
    root.put("px",px);root.put("py",py);root.put("indoors",indoors);root.put("currentHouse",currentHouse);root.put("worldTime",worldTime);root.put("mana",mana);root.put("hp",hp);root.put("level",playerLevel);root.put("exp",exp);
    root.put("gold",gold);root.put("silver",silver);root.put("copper",copper);root.put("kills",kills);root.put("animalKills",animalKills);root.put("nextQuestDay",nextQuestDay);root.put("questTier",questTier);root.put("questProgress",questProgress);root.put("questActive",questActive);
    org.json.JSONObject unique=new org.json.JSONObject();for(String key:definitions.keySet())if(key.matches(".* \\{[0-9]+\\}$")){Item i=definitions.get(key);org.json.JSONObject o=new org.json.JSONObject();o.put("rarity",i.rarity);o.put("slot",i.slot);o.put("dmg",i.dmg);o.put("armor",i.armor);o.put("hp",i.hp);o.put("mana",i.mana);unique.put(key,o);}root.put("uniqueGear",unique);root.put("nextGearId",nextGearId);
    root.put("smithStock",new org.json.JSONArray(java.util.Arrays.asList(smithStock)));root.put("shopStock",new org.json.JSONArray(java.util.Arrays.asList(shopStock)));
    org.json.JSONObject bag=new org.json.JSONObject();for(String name:inventory.keySet())bag.put(name,inventory.get(name));root.put("inventory",bag);
    org.json.JSONObject gear=new org.json.JSONObject();for(String slot:equipped.keySet())gear.put(slot,equipped.get(slot));root.put("equipped",gear);
    org.json.JSONArray bar=new org.json.JSONArray();for(String item:hotbar)bar.put(item==null?org.json.JSONObject.NULL:item);root.put("hotbar",bar);
    org.json.JSONArray enemies=new org.json.JSONArray();for(Mob m:mobs){org.json.JSONObject o=new org.json.JSONObject();
     o.put("kind",m.kind);o.put("x",m.x);o.put("y",m.y);o.put("hp",m.hp);o.put("maxHp",m.maxHp);o.put("level",m.level);o.put("damage",m.damage);o.put("dead",m.dead);enemies.put(o);}root.put("mobs",enemies);
    org.json.JSONArray ground=new org.json.JSONArray();for(Drop d:drops){org.json.JSONObject o=new org.json.JSONObject();o.put("item",d.item);o.put("x",d.x);o.put("y",d.y);ground.put(o);}root.put("drops",ground);
    boolean saved=getContext().getSharedPreferences("pixel_save",MODE_PRIVATE).edit().putString("save",root.toString()).commit();
    pickupNotice=saved?"Spiel gespeichert":"Speichern fehlgeschlagen";noticeTime=3f;
   }catch(Exception ex){pickupNotice="Speichern fehlgeschlagen";noticeTime=3f;}
  }
  boolean hasLoadableSave(){
   try{
    String json=getContext().getSharedPreferences("pixel_save",MODE_PRIVATE).getString("save",null);
    if(json==null)return false;
    org.json.JSONObject root=new org.json.JSONObject(json);
    return root.has("inventory")&&root.has("equipped")&&root.has("hotbar")&&root.has("mobs")&&root.has("drops")&&root.optInt("hp",0)>0&&root.has("px")&&root.has("py");
   }catch(Exception ex){return false;}
  }
  void newCharacterAfterDeath(){restart();creatorOpen=true;startScreen=false;pauseMenu=false;}
  void loadGame(){
   try{
    String json=getContext().getSharedPreferences("pixel_save",MODE_PRIVATE).getString("save",null);
    if(json==null){pickupNotice="Kein Spielstand";noticeTime=3f;return;}
    org.json.JSONObject root=new org.json.JSONObject(json);
    if(!root.has("inventory")||!root.has("equipped")||!root.has("hotbar")||!root.has("mobs")||!root.has("drops"))throw new org.json.JSONException("Incomplete save");
    characterName=root.optString("characterName","Abenteurer");armorStyle=0;characterGender=root.optInt("characterGender",0);hairType=root.optInt("hairType",0);hairColor=root.optInt("hairColor",1);skinColor=root.optInt("skinColor",1);eyeColor=root.optInt("eyeColor",0);combatStyle=root.optInt("combatStyle",0);creatorOpen=false;
    px=(float)root.getDouble("px");py=(float)root.getDouble("py");
    if(!Float.isFinite(px)||!Float.isFinite(py))throw new org.json.JSONException("Invalid position");indoors=root.optBoolean("indoors",false);currentHouse=root.optInt("currentHouse",0);worldTime=(float)root.optDouble("worldTime",0);playerLevel=root.getInt("level");exp=root.getInt("exp");
    gold=root.getInt("gold");silver=root.getInt("silver");copper=root.getInt("copper");kills=root.optInt("kills");animalKills=root.optInt("animalKills");nextQuestDay=root.has("nextQuestDay")?root.optInt("nextQuestDay"):root.optInt("questsCompletedToday")>0?currentQuestDay()+2:0;questTier=root.optInt("questTier");questProgress=root.optInt("questProgress");questActive=root.optBoolean("questActive");
    definitions.keySet().removeIf(k->k.matches(".* \\{[0-9]+\\}$"));
    nextGearId=root.optInt("nextGearId",1);org.json.JSONObject unique=root.optJSONObject("uniqueGear");if(unique!=null){java.util.Iterator<String> ids=unique.keys();while(ids.hasNext()){String id=ids.next();org.json.JSONObject o=unique.getJSONObject(id);Item item=new Item(id,o.getString("rarity"),o.getString("slot"),o.getInt("dmg"),o.getInt("armor"),o.getInt("hp"),0);item.mana=o.optInt("mana");definitions.put(id,item);}}
    org.json.JSONArray savedSmith=root.optJSONArray("smithStock");if(savedSmith!=null){smithStock=new String[savedSmith.length()];for(int j=0;j<smithStock.length;j++)smithStock[j]=savedSmith.getString(j);}
    org.json.JSONArray savedShop=root.optJSONArray("shopStock");if(savedShop!=null){shopStock=new String[savedShop.length()];for(int j=0;j<shopStock.length;j++)shopStock[j]=savedShop.getString(j);}
    inventory.clear();org.json.JSONObject bag=root.getJSONObject("inventory");java.util.Iterator<String> keys=bag.keys();while(keys.hasNext()){String k=keys.next();int count=bag.getInt(k);if(count>0&&definitions.containsKey(k))inventory.put(k,Math.min(count,999999));}
    equipped.clear();org.json.JSONObject gear=root.getJSONObject("equipped");keys=gear.keys();while(keys.hasNext()){String k=keys.next();String item=gear.getString(k);if(definitions.containsKey(item))equipped.put(k,item);}
    java.util.Arrays.fill(hotbar,null);selectedHotbar=-1;org.json.JSONArray bar=root.getJSONArray("hotbar");for(int i=0;i<Math.min(hotbar.length,bar.length());i++)if(!bar.isNull(i))hotbar[i]=bar.getString(i);
    recalcStats();hp=Math.max(0,Math.min(maxHp,root.getInt("hp")));mana=Math.max(0,Math.min(maxMana,root.optInt("mana",maxMana)));cleanupHotbar();
    mobs.clear();org.json.JSONArray enemies=root.getJSONArray("mobs");for(int i=0;i<enemies.length();i++){org.json.JSONObject o=enemies.getJSONObject(i);
     Mob m=new Mob(o.getString("kind"),(float)o.getDouble("x"),(float)o.getDouble("y"));m.hp=o.getInt("hp");m.maxHp=o.getInt("maxHp");m.level=o.getInt("level");m.damage=o.getInt("damage");m.dead=o.getBoolean("dead");mobs.add(m);}
    drops.clear();org.json.JSONArray ground=root.getJSONArray("drops");for(int i=0;i<ground.length();i++){org.json.JSONObject o=ground.getJSONObject(i);drops.add(new Drop(o.getString("item"),(float)o.getDouble("x"),(float)o.getDouble("y"),0));}
    startScreen=false;pauseMenu=false;inventoryOpen=false;shopOpen=false;calendarOpen=false;joyX=joyY=0;previous=System.nanoTime();
    pickupNotice="Spielstand geladen";noticeTime=3f;
   }catch(Exception ex){pickupNotice="Laden fehlgeschlagen";noticeTime=3f;}
  }
  void drawCreator(float vw,float vh){
   drawRect(col("#0b1419"),0,0,vw,vh);
   float left=vw/2-285,top=vh/2-205;
   panel(left,top,570,410);
   // Gilded frame, corner ornaments and a restrained fantasy serif heading.
   drawRect(col("#8b6a39"),left+9,top+9,552,2);
   drawRect(col("#8b6a39"),left+9,top+399,552,2);
   drawRect(col("#8b6a39"),left+9,top+9,2,392);
   drawRect(col("#8b6a39"),left+559,top+9,2,392);
   for(int i=0;i<4;i++){
    float cx=left+(i%2==0?17:553),cy=top+(i<2?17:393);
    paint.setColor(col("#e6bc70"));canvas.drawCircle(cx,cy,5,paint);
    paint.setColor(col("#483a28"));canvas.drawCircle(cx,cy,2,paint);
   }
   paint.setTextAlign(Paint.Align.CENTER);
   paint.setTypeface(Typeface.create("serif",Typeface.BOLD_ITALIC));
   paint.setColor(col("#f5d795"));paint.setTextSize(25);
   canvas.drawText("Charaktererschaffung",vw/2,top+39,paint);
   paint.setTypeface(Typeface.DEFAULT);
   drawRect(col("#b59a68"),left+22,top+55,205,291);
   drawRect(col("#152d2b"),left+25,top+58,199,285);
   drawRect(col("#2c4b43"),left+31,top+64,187,271);
   drawCharacter(left+124,top+202,170,225,0,"idle",elapsed);
   paint.setColor(col("#f0d6a0"));paint.setTextSize(12);
   canvas.drawText("CHARAKTERVORSCHAU",left+124,top+332,paint);
   paint.setColor(col("#c7a977"));canvas.drawText("✦",left+124,top+78,paint);
   paint.setTextSize(11);paint.setColor(col("#e6c993"));paint.setTypeface(Typeface.DEFAULT_BOLD);
   canvas.drawText("CHARAKTERNAME",left+394,top+65,paint);
   drawRect(col("#a88a57"),left+247,top+71,294,33);
   drawRect(col("#1a3033"),left+249,top+73,290,29);
   paint.setColor(Color.WHITE);paint.setTextSize(15);
   canvas.drawText(characterName,left+394,top+94,paint);
   String[] names={"GESCHLECHT","FRISUR","HAUTFARBE","HAARFARBE","KAMPFSTIL"};
   String[] values={characterGender==0?"Maennlich":"Weiblich",hairType==0?"Bob":"Kurzhaar","Variante "+skinColor,"Farbe "+hairColor,new String[]{"Schwert + Schild","Axt + Schild","Streitkolben + Schild"}[combatStyle]};
   for(int i=0;i<5;i++){
    float yy=top+121+i*38;
    paint.setTextSize(12);paint.setTypeface(Typeface.DEFAULT_BOLD);
    paint.setTextAlign(Paint.Align.LEFT);paint.setColor(col("#fff0c7"));canvas.drawText(names[i],left+249,yy+19,paint);
    paint.setTextAlign(Paint.Align.CENTER);
    drawRect(col("#96794c"),left+357,yy,184,29);
    drawRect(col("#20383a"),left+359,yy+2,180,25);
    drawRect(col("#35524c"),left+361,yy+3,176,2);
    paint.setTextSize(17);paint.setColor(col("#f3d48c"));
    canvas.drawText("‹",left+374,yy+21,paint);canvas.drawText("›",left+525,yy+21,paint);
    paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(13);paint.setColor(Color.WHITE);
    canvas.drawText(values[i],left+449,yy+20,paint);
   }
   boolean beginPressed=pressedCreatorButton;float beginOffset=beginPressed?5f:0f;
   drawRect(col(beginPressed?"#776e61":"#d1a75d"),left+246,top+352+beginOffset,296,45-beginOffset);
   drawRect(col(beginPressed?"#403d37":"#76552e"),left+249,top+355+beginOffset,290,39-beginOffset);
   paint.setTypeface(Typeface.create("serif",Typeface.BOLD));
   paint.setColor(col(beginPressed?"#aca8a0":"#fff0c9"));paint.setTextSize(19);
   canvas.drawText("Abenteuer beginnen",left+394,top+381+beginOffset,paint);
   paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.DEFAULT);
  }
  void creatorClick(float x,float y,float vw,float vh){
   float left=vw/2-285,top=vh/2-205;
   if(pressedCreatorButton)return;
   if(x>=left+246&&x<=left+542&&y>=top+352&&y<=top+397){
    pressedCreatorButton=true;pressedCreatorTime=.35f;invalidate();return;
   }
   if(x>=left+248&&x<=left+540&&y>=top+71&&y<=top+104){
    final android.widget.EditText edit=new android.widget.EditText(getContext());edit.setSingleLine(true);edit.setText(characterName);
    new android.app.AlertDialog.Builder(getContext()).setTitle("Charaktername").setView(edit).setPositiveButton("Uebernehmen",(d,w)->{String n=edit.getText().toString().trim();if(!n.isEmpty())characterName=n.length()>18?n.substring(0,18):n;}).setNegativeButton("Abbrechen",null).show();return;
   }
   if(x<left+357||x>left+541)return;
   int index=(int)((y-(top+121))/38);
   if(index<0||index>4||y>top+121+index*38+29)return;
   int delta=x<left+449?-1:1;
   switch(index){
    case 0:characterGender=Math.floorMod(characterGender+delta,2);break;
    case 1:hairType=Math.floorMod(hairType+delta,2);break;
    case 2:skinColor=Math.floorMod(skinColor+delta,11);break;
    case 3:hairColor=Math.floorMod(hairColor+delta,14);break;
    case 4:combatStyle=Math.floorMod(combatStyle+delta,3);break;
    
   }
  }
  int pressedMenuButton=-1;float pressedMenuTime=0f;
  boolean pressedControlsBack=false,pressedCreatorButton=false;
  float pressedControlsTime=0f,pressedCreatorTime=0f;
  void runMenuButton(int i){
   if(i==4){controlsOpen=true;return;}
   if(i==0){if(startScreen){creatorOpen=true;pauseMenu=false;}else{startScreen=false;pauseMenu=false;previous=System.nanoTime();}}
   if(i==1)saveGame();
   if(i==2)loadGame();
   if(i==3){restart();creatorOpen=true;startScreen=false;pauseMenu=false;}
  }
  void drawMenu(float vw,float vh){
   drawRect(col("#101820"),0,0,vw,vh);
   float left=vw/2-250,top=vh/2-208;
   panel(left,top,500,416);drawRect(col("#c6a66c"),left+16,top+77,468,2);drawRect(col("#b38d54"),left+16,top+296,468,2);
   paint.setTextAlign(Paint.Align.CENTER);
   fantasyHeading("Pixel Abenteuer",vw/2,top+47,30);
   paint.setColor(col("#b6c8c2"));paint.setTextSize(13);
   canvas.drawText(startScreen?"Willkommen in deiner Welt":"PAUSE  /  SPIELMENUE",vw/2,top+70,paint);
   String[] buttons={startScreen?"SPIEL STARTEN":"WEITERSPIELEN","SPIEL SPEICHERN","SPIEL LADEN","NEUSTART"};
   for(int i=0;i<buttons.length;i++){float y=top+89+i*49;boolean pressed=pressedMenuButton==i;float offset=pressed?3f:0f;
    drawRect(col(pressed?"#716b60":"#c49b5d"),left+95,y+offset,310,42-offset);
    drawRect(col(pressed?"#29332f":"#294540"),left+98,y+3+offset,304,36-offset);
    paint.setColor(col(pressed?"#a6a7a1":"#f6e7c6"));paint.setTypeface(Typeface.create("serif",Typeface.BOLD));paint.setTextSize(18);canvas.drawText(buttons[i],vw/2,y+26+offset,paint);paint.setTypeface(Typeface.DEFAULT);}
   paint.setColor(col("#e5cc9c"));paint.setTextSize(14);
   canvas.drawText("MUSIKLAUTSTAERKE  "+Math.round(masterMusicVolume*100)+"%",vw/2,top+319,paint);
   float sx=left+110,sy=top+337,sw=280;
   drawRect(col("#111c23"),sx,sy,sw,12);
   drawRect(col("#8c754d"),sx+2,sy+2,(sw-4)*masterMusicVolume,8);
   drawRect(col("#f6e7c6"),sx+masterMusicVolume*sw-5,sy-4,10,20);
   boolean controlsPressed=pressedMenuButton==4;float controlsOffset=controlsPressed?3f:0f;
   drawRect(col(controlsPressed?"#716b60":"#c49b5d"),left+95,top+365+controlsOffset,310,37-controlsOffset);
   drawRect(col(controlsPressed?"#29332f":"#294540"),left+98,top+368+controlsOffset,304,31-controlsOffset);
   paint.setColor(col(controlsPressed?"#a6a7a1":"#f6e7c6"));paint.setTypeface(Typeface.create("serif",Typeface.BOLD));paint.setTextSize(17);
   canvas.drawText("STEUERUNG ANZEIGEN",vw/2,top+389+controlsOffset,paint);paint.setTypeface(Typeface.DEFAULT);

   paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.DEFAULT);
  }
  void controlKey(String key,String meaning,float x,float y,float width){
   drawRect(col("#b58e56"),x,y,width,38);
   drawRect(col("#263f3c"),x+2,y+2,width-4,34);
   paint.setTextAlign(Paint.Align.CENTER);paint.setColor(col("#ffdf9c"));paint.setTypeface(Typeface.DEFAULT_BOLD);
   paint.setTextSize(key.length()>6?11:15);canvas.drawText(key,x+width/2,y+24,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);
   paint.setColor(col("#f3e7cc"));paint.setTextSize(13);canvas.drawText(meaning,x+width+12,y+24,paint);
  }
  void drawControls(float vw,float vh){
   drawRect(col("#101820"),0,0,vw,vh);
   float left=vw/2-290,top=vh/2-208;
   panel(left,top,580,416);
   paint.setTextAlign(Paint.Align.CENTER);fantasyHeading("Steuerung",vw/2,top+39,27);
   paint.setTextAlign(Paint.Align.LEFT);
   controlKey("W A S D","Bewegen",left+28,top+63,102);
   controlKey("SHIFT","Sprinten",left+302,top+63,92);
   controlKey("E","Handeln / Aufheben",left+28,top+110,102);
   controlKey("I","Inventar",left+302,top+110,92);
   controlKey("LEERTASTE","Angreifen",left+28,top+157,102);
   controlKey("MAUS","Angreifen / Antippen",left+302,top+157,92);
   controlKey("1 - 9","Hotbar benutzen",left+28,top+204,102);
   controlKey("ESC","Pause / Zurueck",left+302,top+204,92);
   paint.setColor(col("#d7c7a8"));paint.setTextSize(13);paint.setTextAlign(Paint.Align.CENTER);
   canvas.drawText("Auf dem Handy: Joystick und Bildschirmtasten verwenden.",vw/2,top+281,paint);
   float backOffset=pressedControlsBack?5f:0f;
   drawRect(col(pressedControlsBack?"#716b60":"#c49b5d"),left+135,top+325+backOffset,310,49-backOffset);
   drawRect(col(pressedControlsBack?"#29332f":"#294540"),left+138,top+328+backOffset,304,43-backOffset);
   paint.setColor(col(pressedControlsBack?"#a6a7a1":"#f6e7c6"));paint.setTypeface(Typeface.create("serif",Typeface.BOLD));paint.setTextSize(19);
   canvas.drawText("ZURUECK ZUM ESC-MENUE",vw/2,top+357+backOffset,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);
  }
  void controlsBackClick(float x,float y,float vw,float vh){
   float left=vw/2-290,top=vh/2-208;
   // Only the actual button is clickable; clicks elsewhere keep this screen open.
   if(pressedControlsBack||x<left+135||x>left+445||y<top+325||y>top+374)return;
   pressedControlsBack=true;pressedControlsTime=.35f;invalidate();
  }
  void menuClick(float x,float y,float vw,float vh){
   float top=vh/2-208,left=vw/2-250;
   if(pressedMenuButton>=0)return;
   if(x>=left+95&&x<=left+405&&y>=top+365&&y<=top+402){pressedMenuButton=4;pressedMenuTime=.15f;invalidate();return;}
   if(x>=left+95&&x<=left+405&&y>=top+328&&y<=top+357){
    setMusicVolume((x-(left+110))/280f);return;
   }
   if(x<left+95||x>left+405)return;
   for(int i=0;i<4;i++){float by=top+89+i*49;if(y>=by&&y<=by+42){
    pressedMenuButton=i;pressedMenuTime=.15f;invalidate();return;
   }}
  }
  void drawHotbar(float vw,float vh){
   final float step=39f,slotSize=35f,start=vw/2-hotbar.length*step/2f;
   if(inventoryOpen)canvas.saveLayerAlpha(start-4,vh-51,start+hotbar.length*step+4,vh,155);
   for(int i=0;i<hotbar.length;i++){
    float x=start+i*step,y=vh-43;
    drawRect(inventoryOpen&&selectedHotbar==i?col("#f0ca78"):col("#141c23"),x-2,y-2,slotSize+4,slotSize+4);
    drawRect(col("#a88751"),x,y,slotSize,slotSize);drawRect(col("#203638"),x+3,y+3,slotSize-6,slotSize-6);drawRect(col("#36534d"),x+4,y+4,slotSize-8,3);
    if(hotbar[i]!=null)itemIcon(hotbar[i],x+4,y+1,.95f);
    paint.setColor(Color.WHITE);paint.setTextSize(10);canvas.drawText(""+(i+1),x+2,y+32,paint);
    if(hotbar[i]!=null){paint.setTextSize(9);canvas.drawText(""+inventory.getOrDefault(hotbar[i],0),x+23,y+32,paint);}
   }
   if(inventoryOpen)canvas.restore();
  }
  void healthBar(float x,float y,float width,int current,int maximum){
   float ratio=Math.max(0,Math.min(1,current/(float)Math.max(1,maximum)));
   drawRect(col("#231f20"),x-width/2-2,y-2,width+4,9);
   drawRect(col("#7a2d2b"),x-width/2,y,width,5);
   drawRect(col("#60c86c"),x-width/2,y,width*ratio,5);
  }
  // Camera-centered outdoor minimap, clipped to its frame.
  void drawMinimap(float vw,float vh){
   float size=104f,x=vw-size-12f,y=45f,zoom=.18f;
   drawRect(col("#101b21"),x-4,y-4,size+8,size+8);
   drawRect(col("#b89c69"),x-2,y-2,size+4,size+4);
   canvas.save();
   canvas.clipRect(x,y,x+size,y+size);
   drawRect(col("#416c43"),x,y,size,size);
   // Subtle world grid for orientation.
   paint.setColor(col("#507b50"));paint.setStrokeWidth(1);
   float step=32*zoom,offsetX=(px*zoom)%step,offsetY=(py*zoom)%step;
   for(float gx=x+size/2-offsetX;gx<x+size;gx+=step)canvas.drawLine(gx,y,gx,y+size,paint);
   for(float gy=y+size/2-offsetY;gy<y+size;gy+=step)canvas.drawLine(x,gy,x+size,gy,paint);
   // Fixed world landmarks.
   float fx=x+size/2+(forgeX-px)*zoom,fy=y+size/2+(forgeY-py)*zoom;
   drawRect(col("#e8b975"),fx-5,fy-5,10,10);
   float hx=x+size/2+(homeX-px)*zoom,hy=y+size/2+(homeY-py)*zoom;
   drawRect(col("#c6d5b4"),hx-4,hy-4,8,8);
   float tx=x+size/2+(merchantX-px)*zoom,ty=y+size/2+(merchantY-py)*zoom;
   drawRect(col("#e6d05f"),tx-3,ty-3,6,6);
   for(Decoration d:decorations){
    float dx=x+size/2+(d.x-px)*zoom,dy=y+size/2+(d.y-py)*zoom;
    if(dx<x||dx>x+size||dy<y||dy>y+size)continue;
    drawRect(col("#2f5435"),dx-1,dy-1,3,3);
   }
   for(Mob m:mobs){
    if(m.dead)continue;
    float mx=x+size/2+(m.x-px)*zoom,my=y+size/2+(m.y-py)*zoom;
    if(mx<x||mx>x+size||my<y||my>y+size)continue;
    drawRect(m.hostile()?col("#dc6660"):col("#d2c5a1"),mx-2,my-2,4,4);
   }
   // Player is always at the center.
   drawRect(col("#101b21"),x+size/2-4,y+size/2-4,8,8);
   drawRect(col("#62c9ff"),x+size/2-3,y+size/2-3,6,6);
   canvas.restore();
   paint.setColor(col("#f5e8cb"));paint.setTextSize(10);paint.setTextAlign(Paint.Align.CENTER);
   canvas.drawText("N",x+size/2,y+11,paint);paint.setTextAlign(Paint.Align.LEFT);
  }
  void fantasyName(String value,float x,float y){
   Typeface old=paint.getTypeface();float oldSize=paint.getTextSize();
   paint.setTypeface(Typeface.create("serif",Typeface.BOLD_ITALIC));
   paint.setTextSize(oldSize*.6f);
   outlinedText(value,x,y);
   paint.setTextSize(oldSize);paint.setTypeface(old);
  }
  // Compact keyboard prompt for BlueStacks; coordinates mark the key center.
  void drawInteractionKey(float x,float y){
   Paint.Align oldAlign=paint.getTextAlign();Typeface oldFace=paint.getTypeface();float oldSize=paint.getTextSize();
   drawRect(col("#111c22"),x-12,y-14,24,23);
   drawRect(col("#b89b61"),x-10,y-12,20,19);
   drawRect(col("#263c3b"),x-8,y-10,16,15);
   paint.setColor(col("#fff0c6"));paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(14);
   canvas.drawText("E",x,y+2,paint);
   paint.setTextAlign(oldAlign);paint.setTypeface(oldFace);paint.setTextSize(oldSize);
  }
  void outlinedText(String value,float x,float y){
   int color=paint.getColor();Paint.Style style=paint.getStyle();float width=paint.getStrokeWidth();
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);
   paint.setStrokeWidth(3.2f);paint.setColor(Color.BLACK);canvas.drawText(value,x,y,paint);
   paint.setStyle(Paint.Style.FILL);paint.setColor(color);canvas.drawText(value,x,y,paint);
   paint.setStyle(style);paint.setStrokeWidth(width);
  }
  void drawRect(int col,float x,float y,float w,float h){paint.setColor(col);canvas.drawRect(x,y,x+w,y+h,paint);}
  final HashMap<String,Integer> colorCache=new HashMap<>();
  int col(String hex){Integer value=colorCache.get(hex);if(value!=null)return value;int parsed=Color.parseColor(hex);colorCache.put(hex,parsed);return parsed;}
  String characterPath(String page,String layer,String type,int variant){
   String file="char_a_"+page+"_"+layer+"_"+type+"_v"+String.format(java.util.Locale.US,"%02d",variant)+".png";
   String folder=layer.equals("0bas")?"":layer+"/";
   return "sprites/ManaSeedCharacter/char_a_"+page+"/"+folder+file;
  }
  Bitmap characterSheet(String page,String layer,String type,int variant){
   String key=characterPath(page,layer,type,variant);
   if(characterSheets.containsKey(key))return characterSheets.get(key);
   Bitmap b=null;try(InputStream in=getAssets().open(key)){b=BitmapFactory.decodeStream(in);}catch(Exception ignored){}
   characterSheets.put(key,b);return b;
  }
  void characterLayer(String page,String layer,String type,int variant,int row,int column,float x,float y,float w,float h){
   Bitmap b=characterSheet(page,layer,type,variant);if(b==null)return;
   int cell=b.getWidth()/8;if(cell<=0||b.getHeight()<cell*(row+1))return;
   int left=column*cell,top=row*cell;
   if(left+cell>b.getWidth())return;
   paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setColorFilter(null);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,new Rect(left,top,left+cell,top+cell),new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);
  }
  void drawCharacter(float x,float y,float w,float h,int dir,String action,float time){
   boolean fighting=action.equals("attack")||action.equals("hurt")||action.equals("death");
   String page=fighting?"pONE3":"p1";
   // Mana Seed p1: rows 0-3 = stand/push/pull/jump; rows 4-7 = walk.
   // Sprint uses walk frames 3 and 6 (zero-based columns 2 and 5), not columns 6/7.
   // Each group contains the four facing directions; never use push frames for walking.
   // Sprite-sheet direction order differs from the game's front/back/left/right order.
   // Mana Seed: row 0 front, row 1 back, row 2 right, row 3 left.
   int facing=dir==0?0:dir==1?1:dir==2?3:2;
   boolean moving=action.equals("walk")||action.equals("run");
   int row=fighting?(action.equals("attack")?(attackSequence%2)*4+facing:action.equals("hurt")?facing:4+facing):(moving?4+facing:facing);
   int col=fighting?Math.min(3,Math.max(0,(int)(time*10))):(moving?(action.equals("run")?((int)(elapsed*12)%6):(int)(elapsed*8)%6):0);
   characterLayer(page,"0bas","humn",skinColor,row,col,x,y,w,h);
   characterLayer(page,"1out",equipped.containsKey("Brust")?"fstr":(characterGender==0?"boxr":"undi"),1,row,col,x,y,w,h);
   characterLayer(page,"4har",hairType==0?"bob1":"dap1",hairColor,row,col,x,y,w,h);
   if(equipped.containsKey("Kopf"))characterLayer(page,"5hat","pfht",1,row,col,x,y,w,h);
   if(creatorOpen||fighting){
    String weapon=combatStyle==1?"ax01":combatStyle==2?"mc01":"sw01";
    // Creator preview: draw the weapon AFTER the body and outfit, so the
    // blade and handle cannot disappear behind the character sprite.
    if(creatorOpen||equipped.containsKey("Schild"))characterLayer(fighting?"pONE3":"pONE2","7tlb",equipped.getOrDefault("Schild","").contains("Iron")?"sh03":"sh01",1,fighting?row:facing,fighting?col:0,x,y,w,h);
    // Preview weapon is the foremost layer, including in front of the shield.
    if(creatorOpen){
     // Render the weapon last and slightly enlarged so its blade is not obscured by the body.
     characterLayer("pONE2","6tla",weapon,1,facing,0,x-9f,y+5f,w*1.20f,h*1.20f);
    }
    else characterLayer("pONE3","6tla",weapon,1,row,col,x,y,w,h);
   }
  }
  void drawSprite(String kind,String action,int dir,float x,float y,float w,float h,float time,boolean freeze){
   if(kind.startsWith("player")){drawCharacter(x,y,w,h,dir,action,time);return;}

   String direction=new String[]{"front","back","left","right"}[Math.max(0,Math.min(3,dir))];
   Bitmap b=sprites.get(kind+"_"+action+"_"+direction);
   if(b==null)b=sprites.get(kind+"_idle_"+direction);
   if(b==null){drawRect(kind.startsWith("orc")?col("#71994b"):kind.startsWith("player")?col("#4775a0"):col("#b98d62"),x-w/2,y-h/2,w,h);return;}
   int frameWidth=b.getHeight();int count=Math.max(1,b.getWidth()/Math.max(1,frameWidth));
   int frame=freeze?count-1:((int)(Math.max(0,time)*8))%count;
   paint.setColor(Color.WHITE);paint.setColorFilter(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,new Rect(frame*frameWidth,0,(frame+1)*frameWidth,b.getHeight()),new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);
  }
  // Coordinates are measured against the 42x22 Tiled map (16px per tile).
  // Keep these bounds allocated once rather than recreating them every movement frame.
  // Sprite-alpha collision for object tiles; floors and carpets never collide.
  boolean[] smithyCollisionPixels;
  void buildSmithyCollisionPixels(){
   if(smithyCollisionPixels!=null||smithyLayers==null)return;
   final int width=672,height=352;
   boolean[] mask=new boolean[width*height];
   String[] atlas={"Decorative_cracks","Walls_interior","Forge","Interior_objects"};
   int[] first={1,286,558,654},cols={19,16,8,13},counts={285,272,96,507};
   // 0 Boden, 1 boden2, 2 mehr teppich: always passable.
   // 3 Theke und teppiche: GIDs 654..703 are the counter; GIDs 966+ are carpets.
   // 5 Wände: masonry tiles only; object GIDs on this layer are furniture.
   for(int layerIndex=3;layerIndex<smithyLayers.length;layerIndex++){
    long[] layer=smithyLayers[layerIndex];
    for(int i=0;i<layer.length;i++){
     int tileX=i%42,tileY=i/42;
     long encoded=layer[i],gid=encoded&0x1fffffffL;
     if(gid==0)continue;
     if(layerIndex==3&&!(gid>=654&&gid<=703&&tileY>=9&&tileY<=10&&tileX>=21))continue;
     // The bottom doorway is two tiles wide, regardless of wall art.
     if(layerIndex==5&&tileY>=19&&(tileX==30||tileX==31))continue;
     // Stone masonry must be solid, but do not treat shop decorations
     // placed on the walls layer as a full solid wall.
     boolean masonry=layerIndex==5&&gid<654;
     int set=-1;
     for(int k=0;k<4;k++)if(gid>=first[k]&&gid<first[k]+counts[k]){set=k;break;}
     if(set<0)continue;
     Bitmap bitmap=buildingSprites.get(atlas[set]);
     if(bitmap==null)continue;
     int local=(int)gid-first[set],sx=(local%cols[set])*16,sy=(local/cols[set])*16;
     if(sx+16>bitmap.getWidth()||sy+16>bitmap.getHeight())continue;
     boolean flipH=(encoded&0x80000000L)!=0,flipV=(encoded&0x40000000L)!=0,diag=(encoded&0x20000000L)!=0;
     for(int yy=0;yy<16;yy++)for(int xx=0;xx<16;xx++){
      int u=xx,v=yy;
      if(flipH)u=15-u;
      if(flipV)v=15-v;
      if(diag){int tmp=u;u=v;v=tmp;}
      if(masonry||Color.alpha(bitmap.getPixel(sx+u,sy+v))>=96)
       mask[(tileY*16+yy)*width+tileX*16+xx]=true;
     }
    }
   }
   smithyCollisionPixels=mask;
  }
  boolean interiorObstacle(float x,float y){
   float tx=x-60f,ty=y-174f;
   // The player has a small circular foot area.
   if(tx<8||tx>=664||ty<8||ty>=344)return true;
   buildSmithyCollisionPixels();
   if(smithyCollisionPixels!=null){
    final int radius=6;
    int cx=Math.round(tx),cy=Math.round(ty);
    for(int dy=-radius;dy<=radius;dy+=3)for(int dx=-radius;dx<=radius;dx+=3){
     if(dx*dx+dy*dy>radius*radius)continue;
     int sx=cx+dx,sy=cy+dy;
     if(sx<0||sx>=672||sy<0||sy>=352||smithyCollisionPixels[sy*672+sx])return true;
    }
   }
   // NPCs have separate small footprints.
   if(Math.hypot(x-237f,y-337f)<17f)return true;
   if(Math.hypot(x-570f,y-335f)<16f)return true;
   if(Math.hypot(x-622f,y-431f)<21f)return true;
   return false;
  }
  // Attack animation window; the next group can act immediately afterward.
  void update(float dt){
   if(pressedControlsBack){pressedControlsTime-=dt;if(pressedControlsTime<=0f){pressedControlsBack=false;controlsOpen=false;}}
   if(pressedCreatorButton){pressedCreatorTime-=dt;if(pressedCreatorTime<=0f){pressedCreatorButton=false;creatorOpen=false;startScreen=false;pauseMenu=false;previous=System.nanoTime();}}
   gearTooltipTime=Math.max(0f,gearTooltipTime-dt);
   if(!inventoryOpen&&!shopOpen){inspectedGear=null;gearTooltipTime=0f;}
   if(pressedMenuButton>=0){pressedMenuTime-=dt;if(pressedMenuTime<=0f){int button=pressedMenuButton;pressedMenuButton=-1;runMenuButton(button);}}
   if(!startScreen&&!pauseMenu&&!creatorOpen&&!inventoryOpen&&!shopOpen&&!calendarOpen){
    int previousDay=(int)(worldTime/240f);
    worldTime+=dt;
    if((int)(worldTime/240f)>previousDay){
     mobs.removeIf(mob->mob.dead);
     spawnMobs();
    }
    stockRefreshTimer+=dt;
    if(stockRefreshTimer>=STOCK_REFRESH_SECONDS){
     stockRefreshTimer-=STOCK_REFRESH_SECONDS;
     refreshMerchantStocks();
    }
   }
   updateMusic(dt);
   adventurerSpeechTime=Math.max(0,adventurerSpeechTime-dt);
   elapsed+=dt;noticeTime=Math.max(0,noticeTime-dt);swordTimer=Math.max(0,swordTimer-dt);sword=swordTimer>0;attackCooldown=Math.max(0,attackCooldown-dt);attackAnim=Math.max(0,attackAnim-dt);hurtAnim=Math.max(0,hurtAnim-dt);playerHealthVisible=Math.max(0,playerHealthVisible-dt);playerDamageTime=Math.max(0,playerDamageTime-dt);
   if(hp<=0){deathAnim+=dt;if(!hasLoadableSave())newCharacterAfterDeath();return;}if(inventoryOpen||shopOpen||pauseMenu||startScreen||creatorOpen||calendarOpen)return;
   float mx=joyX,my=joyY,length=(float)Math.hypot(mx,my);
   if(length>1){mx/=length;my/=length;}
   if(indoors){
    if(Math.abs(mx)+Math.abs(my)>.08f)playerDir=direction(mx,my);
    float nx=Math.max(72,Math.min(718,px+mx*(sprintHeld?205:140)*dt));
    float ny=Math.max(194,Math.min(525,py+my*(sprintHeld?205:140)*dt));
    // Partition occupies x=375..402 in interior player coordinates.
    // The middle opening (y=354..444) is deliberately passable.
    if(!interiorObstacle(nx,py))px=nx;
    if(!interiorObstacle(px,ny))py=ny;
    if(py>DOOR_IN_Y-8&&Math.abs(px-DOOR_IN_X)<17&&my>0)enterOrLeave();
    return;
   }
   if(Math.abs(mx)+Math.abs(my)>.08f){
    playerDir=Math.abs(mx)>Math.abs(my)?(mx<0?2:3):(my<0?1:0);
    float step=(sprintHeld?205:140)*dt/3f;
    for(int sub=0;sub<3;sub++){
     float nx=px+mx*step,ny=py+my*step;
     if(!blocked(nx,py)&&!mobFootBlocked(nx,py,null,12f))px=nx;
     if(!blocked(px,ny)&&!mobFootBlocked(px,ny,null,12f))py=ny;
     if(Math.abs(px-(forgeX+DOOR_OUT_X))<22&&py>forgeY+DOOR_OUT_Y-22&&py<forgeY+DOOR_OUT_Y+10&&my<0){enterOrLeave();break;}
    }
   }
   if(indoors)return;
   ArrayList<Mob> engagedOrcs=new ArrayList<>();
   if(!safeZone(px,py))for(Mob candidate:mobs){
    if(!candidate.dead&&candidate.hostile()&&!safeZone(candidate.x,candidate.y)
       &&Math.hypot(px-candidate.x,py-candidate.y)<285f)engagedOrcs.add(candidate);
   }
   ArrayList<Mob> attackingOrcs=new ArrayList<>();
   int activeStrikes=0;
   for(Mob candidate:engagedOrcs)if(candidate.attackTimer>1.15f)activeStrikes++;
   // No shared round pause: fill open attack slots as soon as strikes finish.
   if(activeStrikes<2){
    Collections.shuffle(engagedOrcs,random);
    for(Mob candidate:engagedOrcs){
     if(activeStrikes+attackingOrcs.size()>=2)break;
     if(Math.hypot(px-candidate.x,py-candidate.y)<=48f
        &&candidate.hurtTimer<=0f&&candidate.attackTimer<=0f)
      attackingOrcs.add(candidate);
    }
   }
   int pathSearchBudget=2; // Never start dozens of expensive A* searches in one frame.
   for(Mob m:mobs){
    if(m.dead){m.deathTimer+=dt;continue;}
    m.anim+=dt;m.attackTimer=Math.max(0,m.attackTimer-dt);m.hurtTimer=Math.max(0,m.hurtTimer-dt);m.healthVisible=Math.max(0,m.healthVisible-dt);m.damageNumberTime=Math.max(0,m.damageNumberTime-dt);
    float distance=(float)Math.hypot(px-m.x,py-m.y);
    float vx=0,vy=0;String action="idle";
    if(m.hurtTimer>0){action="hurt";}
    else if(m.hostile()&&distance<(m.kind.equals("boar")?135:285)&&!safeZone(m.x,m.y)&&!safeZone(px,py)){
     m.dir=direction(px-m.x,py-m.y);
     if(attackingOrcs.contains(m)){
      action="attack";
      int dealt=Math.max(1,m.damage);
      hp=Math.max(0,hp-Math.max(1,dealt-armorValue()));playHit(true);
      lastPlayerDamage=dealt;playerDamageTime=.9f;hurtAnim=.32f;playerHealthVisible=3f;
      m.attackTimer=1.5f;
     }else if(distance>43){
      vx=(px-m.x)/Math.max(1,distance)*75;vy=(py-m.y)/Math.max(1,distance)*75;action="run";
     }else{
      // Wait for the next turn: idle or circle the player instead of attacking together.
      float orbit=(mobs.indexOf(m)%2==0?1f:-1f);
      vx=-(py-m.y)/Math.max(1,distance)*orbit*23f;
      vy=(px-m.x)/Math.max(1,distance)*orbit*23f;
      action="walk";
     }
    }else if(m.leader!=null&&!m.leader.dead&&Math.hypot(m.x-m.leader.x,m.y-m.leader.y)>42){
     float dx=m.leader.x-m.x,dy=m.leader.y-m.y,len=(float)Math.hypot(dx,dy);vx=dx/Math.max(1,len)*55;vy=dy/Math.max(1,len)*55;action="walk";
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
    if(Math.abs(vx)+Math.abs(vy)>1){m.pathTimer-=dt;float tx=m.hostile()&&distance<285?px:m.x+vx*1.8f,ty=m.hostile()&&distance<285?py:m.y+vy*1.8f;if(m.pathTimer<=0){if(distance<430f){if(pathSearchBudget>0){pathSearchBudget--;planMobPath(m,tx,ty);}else m.pathTimer=.12f+random.nextFloat()*.12f;}else{m.path.clear();m.pathIndex=0;m.pathTimer=1.5f+random.nextFloat();}}while(m.pathIndex<m.path.size()&&Math.hypot(m.x-m.path.get(m.pathIndex)[0],m.y-m.path.get(m.pathIndex)[1])<10)m.pathIndex++;if(m.pathIndex<m.path.size()){float[] wp=m.path.get(m.pathIndex);float dx=wp[0]-m.x,dy=wp[1]-m.y,len=(float)Math.hypot(dx,dy);if(len>0){float speed=m.hostile()?75f:Math.max(35f,(float)Math.hypot(vx,vy));vx=dx/len*speed;vy=dy/len*speed;}}}
     if(safeZone(m.x,m.y)||safeZone(m.x+vx*dt,m.y+vy*dt)){vx=0;vy=0;m.vx=0;m.vy=0;}
    if(Math.abs(vx)+Math.abs(vy)>1){
     float mobRadius=mobFootRadius(m),playerSeparation=mobRadius+12f;
     float step=dt/3f;
     for(int sub=0;sub<3;sub++){
      float nx=m.x+vx*step,ny=m.y+vy*step;
      if(!blocked(nx,m.y)&&!mobFootBlocked(nx,m.y,m,mobRadius)&&Math.hypot(nx-px,m.y-py)>playerSeparation&&(!m.hostile()||!safeZone(nx,m.y)))m.x=nx;else {m.decision=0;m.pathTimer=0;}
      if(!blocked(m.x,ny)&&!mobFootBlocked(m.x,ny,m,mobRadius)&&Math.hypot(m.x-px,ny-py)>playerSeparation&&(!m.hostile()||!safeZone(m.x,ny)))m.y=ny;else {m.decision=0;m.pathTimer=0;}
     }
     m.dir=direction(vx,vy);
    }
    if(!m.action.equals(action)){m.action=action;m.anim=0;}
   }
  }
  void planMobPath(Mob m,float tx,float ty){
    // Straight, unobstructed paths do not need an A* search or allocations.
    float dxDirect=tx-m.x,dyDirect=ty-m.y;
    float lengthDirect=(float)Math.hypot(dxDirect,dyDirect);
    if(lengthDirect<170f){
     boolean clear=true;
     int probes=Math.max(1,(int)(lengthDirect/18f));
     for(int p=1;p<=probes;p++){
      float xx=m.x+dxDirect*p/probes,yy=m.y+dyDirect*p/probes;
      if(blocked(xx,yy)||(m.hostile()&&safeZone(xx,yy))){clear=false;break;}
     }
     if(clear){m.path.clear();m.pathIndex=0;m.pathTimer=.8f+random.nextFloat()*.35f;return;}
    }
    final int cell=24,cols=(WORLD_W+cell-1)/cell,rows=(WORLD_H+cell-1)/cell,total=cols*rows;
    int sx=Math.max(0,Math.min(cols-1,(int)(m.x/cell))),sy=Math.max(0,Math.min(rows-1,(int)(m.y/cell)));
    int gx=Math.max(0,Math.min(cols-1,(int)(tx/cell))),gy=Math.max(0,Math.min(rows-1,(int)(ty/cell)));
    int start=sy*cols+sx,goal=gy*cols+gx;
    float[] cost=new float[total];java.util.Arrays.fill(cost,Float.POSITIVE_INFINITY);
    int[] parent=new int[total];java.util.Arrays.fill(parent,-1);
    boolean[] closed=new boolean[total];java.util.PriorityQueue<int[]> open=new java.util.PriorityQueue<>((a,b)->Integer.compare(a[1],b[1]));
    cost[start]=0;open.add(new int[]{start,0});int reached=start,best=Math.abs(sx-gx)+Math.abs(sy-gy),expanded=0;
    int[] ox={1,-1,0,0,1,1,-1,-1},oy={0,0,1,-1,1,-1,1,-1};
    while(!open.isEmpty()&&expanded++<1400){
     int node=open.poll()[0];if(closed[node])continue;closed[node]=true;
     int cx=node%cols,cy=node/cols,dist=Math.abs(cx-gx)+Math.abs(cy-gy);
     if(dist<best){best=dist;reached=node;}if(node==goal){reached=node;break;}
     for(int d=0;d<8;d++){
      int nx=cx+ox[d],ny=cy+oy[d];if(nx<0||ny<0||nx>=cols||ny>=rows)continue;
      float wx=nx*cell+cell/2f,wy=ny*cell+cell/2f;
      if(blocked(wx,wy)||(m.hostile()&&safeZone(wx,wy)))continue;
      if(d>=4&&(blocked((cx+ox[d])*cell+cell/2f,cy*cell+cell/2f)||blocked(cx*cell+cell/2f,(cy+oy[d])*cell+cell/2f)))continue;
      int next=ny*cols+nx;float ng=cost[node]+(d>=4?1.4142f:1f);
      if(ng>=cost[next])continue;cost[next]=ng;parent[next]=node;
      open.add(new int[]{next,(int)(ng*10+10*Math.hypot(nx-gx,ny-gy))});
     }
    }
    java.util.ArrayList<float[]> route=new java.util.ArrayList<>();int n=reached;
    while(n!=start&&n>=0&&route.size()<100){route.add(0,new float[]{(n%cols)*cell+cell/2f,(n/cols)*cell+cell/2f});n=parent[n];}
    m.path=route;m.pathIndex=0;m.pathTimer=.7f+random.nextFloat()*.4f;
   }
   int direction(float vx,float vy){return Math.abs(vx)>Math.abs(vy)?(vx<0?2:3):(vy<0?1:0);}
  void attack(){
   if(hp<=0||attackCooldown>0||inventoryOpen||shopOpen||pauseMenu||startScreen||creatorOpen||indoors)return;
   attackCooldown=.42f;attackAnim=.4f;attackSequence++;swordTimer=3f;sword=true;
   float fx=playerDir==2?-1:playerDir==3?1:0,fy=playerDir==1?-1:playerDir==0?1:0;
   for(Mob m:mobs){
    if(m.dead)continue;
    float dx=m.x-px,dy=m.y-py,d=(float)Math.hypot(dx,dy);
    if(d>64||d<.01f)continue;
    if((dx*fx+dy*fy)/d<-.1f)continue;
    m.hp-=attackPower;playHit(false);m.lastDamage=attackPower;m.damageNumberTime=.9f;m.hurtTimer=.28f;m.healthVisible=3f;m.anim=0;
    if(m.hp<=0){m.dead=true;m.deathTimer=0;if(m.hostile()){kills++;if(questActive&&questProgress<questGoal())questProgress++;}else animalKills++;earnExp((m.hostile()?8:3)+m.level*2);loot(m);}
   }
  }
  int calendarDay(){return Math.max(0,(int)(worldTime/240f));}
  String calendarEvent(int absoluteDay){return ""; /* Future timed events use absolute game days. */}
  void drawCalendar(float vw,float vh){
   float l=vw/2-252,t=vh/2-195;
   panel(l,t,504,390);
   int lived=calendarDay(),month=lived/28+1,day=lived%28+1;
   String[] weekdays={"Mo","Di","Mi","Do","Fr","Sa","So"};
   paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT_BOLD);
   paint.setColor(col("#f1d18b"));paint.setTextSize(24);canvas.drawText("KALENDER",vw/2,t+37,paint);
   paint.setTextSize(15);paint.setColor(Color.WHITE);canvas.drawText("Monat "+month+"  |  Tag "+day+"  |  "+(lived+1)+". Lebenstag",vw/2,t+64,paint);
   float phase=(worldTime%240f)/240f;
   String timeOfDay=phase<.25f?"Morgen":phase<.5f?"Tag":phase<.75f?"Abend":"Nacht";
   paint.setTextSize(13);canvas.drawText(timeOfDay+"  -  "+(int)(phase*24)+":00 Spielzeit",vw/2,t+86,paint);
   for(int i=0;i<7;i++){paint.setColor(col("#d3b978"));paint.setTextSize(13);canvas.drawText(weekdays[i],l+56+i*65,t+117,paint);}
   for(int d=0;d<28;d++){
    int col=d%7,row=d/7;float x=l+35+col*65,y=t+130+row*47;
    boolean today=d+1==day;
    drawRect(today?col("#8c713f"):col("#253a36"),x,y,43,38);
    paint.setColor(today?Color.WHITE:col("#d5dfd7"));paint.setTextSize(15);canvas.drawText(""+(d+1),x+21,y+24,paint);
    if(!calendarEvent((month-1)*28+d).isEmpty()){paint.setColor(col("#e8b85a"));canvas.drawCircle(x+36,y+8,3,paint);}
   }
   paint.setColor(col("#c9d5ce"));paint.setTextSize(13);canvas.drawText("K zum Schliessen  |  Ereignisse folgen spaeter",vw/2,t+353,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);
  }
  void restart(){
   hairType=0;hairColor=1;skinColor=1;eyeColor=0;combatStyle=0;armorStyle=0;characterGender=0;characterName="Abenteurer";creatorOpen=false;calendarOpen=false;sprintHeld=false;attackSequence=0;
   indoors=false;currentHouse=0;playerLevel=1;exp=0;attackPower=3;maxHp=12;hp=maxHp;maxMana=20;mana=20;worldTime=0;stockRefreshTimer=0;refreshMerchantStocks();equipped.clear();copper=silver=gold=0;java.util.Arrays.fill(hotbar,null);selectedHotbar=-1;playerDamageTime=0;px=180;py=180;joyX=joyY=0;deathAnim=0;swordTimer=0;sword=false;attackAnim=0;playerHealthVisible=0;kills=animalKills=0;questTier=questProgress=nextQuestDay=0;questActive=false;mobs.clear();drops.clear();inventory.clear();inventoryOpen=false;shopOpen=false;pauseMenu=false;spawnMobs();
  }
  // Experimental fixed 2.5D camera: ground is foreshortened, sprites stay upright.
  static final float CAMERA_GROUND_Y=0.82f;
  final HashMap<String,Float> mobFootRadiusCache=new HashMap<>();
  float mobFootRadius(Mob m){
   Float cached=mobFootRadiusCache.get(m.kind);
   if(cached==null){
    float size=m.kind.startsWith("orc")?79:m.kind.equals("boar")?45:m.kind.equals("deer")?43:36;
    Bitmap b=sprites.get(m.kind+"_idle_front");
    float radius=size*.22f;
    if(b!=null){
     int fw=Math.min(b.getHeight(),b.getWidth()),left=fw,right=0;
     // Scan the sprite only once per species, never during movement.
     for(int yy=b.getHeight()*3/4;yy<b.getHeight();yy+=2)
      for(int xx=0;xx<fw;xx+=2)if(Color.alpha(b.getPixel(xx,yy))>100){left=Math.min(left,xx);right=Math.max(right,xx);}
     if(right>left)radius=Math.max(5f,Math.min(size*.32f,size*(right-left)/(float)fw*.42f));
    }
    cached=radius;mobFootRadiusCache.put(m.kind,cached);
   }
   return cached*(m.alpha?1.28f:1f);
  }
  boolean mobFootBlocked(float x,float y,Mob ignore,float radius){
   for(Mob other:mobs)if(other!=ignore&&!other.dead){
    if(Math.abs(x-other.x)>75f||Math.abs(y-other.y)>75f)continue;
    float otherRadius=mobFootRadius(other),r=radius+otherRadius;
    if(Math.abs(x-other.x)<r&&Math.abs(y-(other.y+otherRadius*.3f))<r*.65f)return true;
   }
   return false;
  }
  String mobDisplayName(Mob m){
   String kind=m.kind;
   String name=kind.startsWith("orc")?"Ork":kind.equals("boar")?"Wildschwein":kind.equals("deer")?"Hirsch":kind.equals("black_grouse")?"Birkhuhn":kind.equals("rabbit")?"Kaninchen":kind.equals("fox")?"Fuchs":kind.replace('_',' ');
   return m.alpha?"Alpha "+name:name;
  }
  void groundShadow(float x,float y,float rx,float ry){
   paint.setColor(Color.argb(170,12,25,17));paint.setStyle(Paint.Style.FILL);
   canvas.drawOval(new RectF(x-rx,y-ry,x+rx,y+ry),paint);
  }
  void billboard(String kind,String action,int dir,float x,float y,float w,float h,float time,boolean freeze){
   canvas.save();canvas.scale(1f,1f/CAMERA_GROUND_Y,x,y);
   drawSprite(kind,action,dir,x,y,w,h,time,freeze);
   canvas.restore();
  }
  @Override protected void onDraw(Canvas c){
   canvas=c;float dt=Math.min(.05f,(System.nanoTime()-previous)/1000000000f);previous=System.nanoTime();update(dt);
   float w=getWidth(),h=getHeight(),scale=Math.min(w/800f,h/450f),vw=w/scale,vh=h/scale;
   paint.reset();paint.setAntiAlias(false);paint.setFilterBitmap(false);paint.setColor(Color.WHITE);
   paint.setColorFilter(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
   // Clear the entire frame, including any emulator-provided dirty region.
   c.drawColor(col("#47884a"),android.graphics.PorterDuff.Mode.SRC);c.save();c.scale(scale,scale);c.save();c.scale(1.10f,1.10f,vw*.5f,vh*.62f);c.save();c.translate(vw/2-px,vh*.62f-py);c.scale(1f,CAMERA_GROUND_Y,px,py);
   int startX=(int)Math.floor((px-vw/2)/32f)*32-32,endX=(int)Math.ceil((px+vw/2)/32f)*32+32;
   int startY=(int)Math.floor((py-vh/2)/32f)*32-32,endY=(int)Math.ceil((py+vh/2)/32f)*32+32;
   for(int x=startX;x<endX;x+=32)for(int y=startY;y<endY;y+=32){
    int n=Math.floorMod((x/32)*37+(y/32)*19+((x/128)*11)+((y/128)*7),13);drawRect(n<3?col("#407d46"):n<6?col("#448249"):col("#47884a"),x,y,32,32);
    if(n==1)drawRect(col("#6a9a53"),x+8,y+7,5,4);
   }
   for(Decoration d:decorations){
    if(d.x+d.w<px-vw/2-60||d.x-d.w>px+vw/2+60||d.y+d.h<py-vh/2-60||d.y-d.h>py+vh/2+60)continue;
    if(Math.hypot(d.x-forgeX,d.y-(forgeY+84))<82)continue;
    if(d.image!=null){paint.setColor(Color.WHITE);paint.setColorFilter(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);c.save();c.scale(1f,1f/CAMERA_GROUND_Y,d.x,d.y+d.h*.2f);c.drawBitmap(d.image,null,new RectF(d.x-d.w/2,d.y-d.h*.8f,d.x+d.w/2,d.y+d.h*.2f),paint);c.restore();}
    else drawRect(col("#82776b"),d.x-d.w/2,d.y-d.h/2,d.w,d.h);
   }
   drawHouse(forgeX,forgeY,true);
   billboard("orc2","idle",0,merchantX,merchantY,79,79,elapsed,false);
   paint.setTextAlign(Paint.Align.CENTER);paint.setColor(col("#ffe7aa"));paint.setTextSize(13);
   fantasyName("Orc-Händler",merchantX,merchantY-39);paint.setTextAlign(Paint.Align.LEFT);
   if(Math.hypot(px-merchantX,py-merchantY)<50&&!shopOpen){paint.setColor(Color.WHITE);paint.setTextSize(12);
    drawInteractionKey(merchantX,merchantY-58);}
   for(Drop d:drops){
    if(d.x<px-vw/2-35||d.x>px+vw/2+35||d.y<py-vh/2-35||d.y>py+vh/2+35)continue;
    float bob=(float)Math.sin(elapsed*3+d.bob)*2;
    drawRect(col("#28342b"),d.x-11,d.y+7,22,5);itemIcon(d.item,d.x-10,d.y-15+bob,.82f);
   }
   // Day-night tint on terrain and decorations, behind creatures and UI.
   float phase=(worldTime%240f)/240f;
   float night=(float)Math.max(0,Math.min(1,(-Math.cos(phase*6.2831853f)-.10f)/.85f));
   paint.setColor(Color.argb((int)(night*70),13,21,52));paint.setStyle(Paint.Style.FILL);
   c.drawRect(px-vw/2-8,py-vh/2-8,px+vw/2+8,py+vh/2+8,paint);
   for(Mob m:mobs){
    if(m.dead&&m.deathTimer>2.5f)continue;
    if(m.x<px-vw/2-90||m.x>px+vw/2+90||m.y<py-vh/2-90||m.y>py+vh/2+90)continue;
    String action=m.dead?"death":m.hurtTimer>0?"hurt":m.action;
    float size=(m.kind.startsWith("orc")?79:m.kind.equals("boar")?45:m.kind.equals("deer")?43:36)*(m.alpha?1.28f:1f);
    billboard(m.kind,action,m.dir,m.x,m.y,size,size,m.dead?m.deathTimer:m.anim,m.dead&&m.deathTimer>.7f);
    if(!m.dead){paint.setColor(Color.WHITE);paint.setTextSize(12);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(10);outlinedText(mobDisplayName(m)+"  Lv. "+m.level,m.x,m.y-size*(m.hostile()?.53f:.64f));paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);}
    if(!m.dead&&m.hostile()&&!indoors&&!safeZone(px,py)&&!safeZone(m.x,m.y)
       &&Math.hypot(px-m.x,py-m.y)<285f){
     paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT_BOLD);
     paint.setTextSize(24);paint.setColor(col(m.attackTimer>1.15f?"#ff3b36":"#ffdb55"));
     outlinedText("!",m.x,m.y-size*.64f-27);
     paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);
    }
    if(m.healthVisible>0&&(!m.dead||m.deathTimer<1f))healthBar(m.x,m.y-size*.65f-12,42,m.hp,m.maxHp);
    damageText(m.x,m.y-size*.65f-35,m.lastDamage,m.damageNumberTime);
   }
   String playerKind=sword?"player_sword":"player_unarmed";
   String playerAction=hp<=0?"death":hurtAnim>0?"hurt":attackAnim>0&&sword?"attack":Math.abs(joyX)+Math.abs(joyY)>.08f?(sprintHeld?"run":"walk"):"idle";
   if(hp>0){groundShadow(px,py+24,13.3f,4.2f);billboard(playerKind,playerAction,playerDir,px,py,80.64f,80.64f,attackAnim>0?.4f-attackAnim:elapsed,false);}
   if(playerHealthVisible>0)healthBar(px,py-56,52,hp,maxHp);
   damageText(px,py-66,lastPlayerDamage,playerDamageTime);
   Drop nearby=nearestDrop();
   if(nearby!=null&&!inventoryOpen){drawRect(col("#182923"),px-87,py-78,174,25);paint.setColor(col("#fff0c5"));paint.setTextSize(13);paint.setTextAlign(Paint.Align.CENTER);
    canvas.drawText("E  "+nearby.item+" aufheben",px,py-60,paint);paint.setTextAlign(Paint.Align.LEFT);}
   c.restore();
   c.restore(); // End world-only zoom; UI stays at original screen scale.
   if(indoors)drawInterior(vw,vh);else drawClouds(vw,vh);
   drawRect(col("#142b24"),0,0,vw,37);paint.setColor(Color.WHITE);paint.setTextSize(18);
   c.drawText("Lv. "+playerLevel+" EXP "+exp+"/"+expNeeded()+" DMG "+attackPower+" DEF "+armorValue(),12,25,paint);
   itemIcon("Golden Coin",vw-202,7,.85f);itemIcon("Silver Coin",vw-137,7,.85f);itemIcon("Copper Coin",vw-72,7,.85f);
   paint.setColor(Color.WHITE);paint.setTextSize(15);c.drawText(""+gold,vw-178,25,paint);c.drawText(""+silver,vw-113,25,paint);c.drawText(""+copper,vw-48,25,paint);
   // Quest tracker only while exploring the outdoor world.
   if(questActive&&!indoors&&!startScreen&&!pauseMenu&&!inventoryOpen&&!shopOpen){
    float qx=12,qy=47,qw=247;
    drawRect(col("#111b21"),qx,qy,qw,71);
    drawRect(col("#9c8557"),qx,qy,qw,3);
    paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(14);
    paint.setColor(col("#f3d18a"));canvas.drawText("AKTIVE QUEST",qx+10,qy+22,paint);
    paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(13);
    paint.setColor(Color.WHITE);canvas.drawText("Abenteurer: Orkjagd",qx+10,qy+42,paint);
    paint.setColor(questProgress>=questGoal()?col("#9bf09c"):col("#d7e4db"));
    canvas.drawText(questProgress>=questGoal()?"Fertig! Beim Abenteurer abgeben":("Orks besiegt: "+questProgress+" / "+questGoal()),qx+10,qy+60,paint);
   }
   if(!indoors&&!startScreen&&!pauseMenu&&!inventoryOpen&&!shopOpen)drawMinimap(vw,vh);
   if(!inventoryOpen)drawHotbar(vw,vh);
   if(inventoryOpen)drawInventory(vw,vh);
   if(calendarOpen&&!creatorOpen&&!startScreen&&!pauseMenu)drawCalendar(vw,vh);
   if(startScreen||pauseMenu){if(controlsOpen)drawControls(vw,vh);else drawMenu(vw,vh);}
   if(creatorOpen)drawCreator(vw,vh);
   if(shopOpen)drawShop(vw,vh);
   // Notifications are drawn after every overlay, so shop and inventory cannot hide them.
   if(noticeTime>0){
    paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(17);
    float noticeWidth=Math.min(vw-24,paint.measureText(pickupNotice)+24);
    drawRect(col("#101820"),8,43,noticeWidth,32);
    drawRect(col("#b89b61"),10,45,noticeWidth-4,28);
    drawRect(col("#263c3b"),12,47,noticeWidth-8,24);
    paint.setColor(col("#f7e0a6"));c.drawText(pickupNotice,20,65,paint);
    paint.setTypeface(Typeface.DEFAULT);
   }
   if(hp<=0){
    float left=vw/2-190,top=vh/2-100;
    panel(left,top,380,200);fantasyHeading("CHARAKTER GESTORBEN",vw/2,top+38,21);
    drawRect(col("#a88751"),left+25,top+64,330,48);drawRect(col("#203638"),left+28,top+67,324,42);
    paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(18);c.drawText("SPIELSTAND LADEN",vw/2,top+95,paint);
    drawRect(col("#a88751"),left+25,top+126,330,48);drawRect(col("#203638"),left+28,top+129,324,42);
    c.drawText("NEUER CHARAKTER",vw/2,top+157,paint);paint.setTextAlign(Paint.Align.LEFT);
   }
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
   showGearTooltip(hoverX,hoverY,vw,vh);
   invalidate();
  }
  @Override public boolean onHoverEvent(MotionEvent e){
   if(inventoryOpen){updateInventoryHover(e);return true;}
   if(shopOpen){float sc=Math.min(getWidth()/800f,getHeight()/450f);showShopTooltip(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);return true;}
   return super.onHoverEvent(e);
  }
  @Override public boolean onGenericMotionEvent(MotionEvent e){
   if(inventoryOpen&&(e.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS||e.getActionMasked()==MotionEvent.ACTION_DOWN)&&(e.getActionButton()==MotionEvent.BUTTON_SECONDARY||(e.getButtonState()&MotionEvent.BUTTON_SECONDARY)!=0)){
    float sc=Math.min(getWidth()/800f,getHeight()/450f);showGearTooltip(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);invalidate();return true;
   }
   if(shopOpen&&(e.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS||e.getActionMasked()==MotionEvent.ACTION_DOWN)&&(e.getActionButton()==MotionEvent.BUTTON_SECONDARY||(e.getButtonState()&MotionEvent.BUTTON_SECONDARY)!=0)){float sc=Math.min(getWidth()/800f,getHeight()/450f);showShopTooltip(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);return true;}
   if((inventoryOpen||shopOpen)&&e.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS&&e.getActionButton()==MotionEvent.BUTTON_TERTIARY){float sc=Math.min(getWidth()/800f,getHeight()/450f);if(shopOpen)showShopTooltip(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);else showGearTooltip(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);return true;}
   if(inventoryOpen){updateInventoryHover(e);return true;}
   if(shopOpen&&e.getActionMasked()==MotionEvent.ACTION_HOVER_MOVE){float sc=Math.min(getWidth()/800f,getHeight()/450f);showShopTooltip(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);return true;}
   if((e.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE&&e.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS
      &&e.getActionButton()==MotionEvent.BUTTON_PRIMARY){attack();return true;}
   return super.onGenericMotionEvent(e);
  }
  @Override public boolean onTouchEvent(MotionEvent e){
   int type=e.getActionMasked();
   if(inventoryOpen&&(type==MotionEvent.ACTION_MOVE||type==MotionEvent.ACTION_HOVER_MOVE)){updateInventoryHover(e);return true;}
   if(type==MotionEvent.ACTION_MOVE&&(startScreen||pauseMenu)&&!creatorOpen){
    float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;
    float top=vh/2-208,left=vw/2-250;
    if(yy>=top+316&&yy<=top+353&&xx>=left+95&&xx<=left+405)setMusicVolume((xx-(left+110))/280f);
    return true;
   }
   if(inventoryOpen&&(type==MotionEvent.ACTION_DOWN||type==MotionEvent.ACTION_BUTTON_PRESS)&&((e.getButtonState()&MotionEvent.BUTTON_SECONDARY)!=0||e.getActionButton()==MotionEvent.BUTTON_SECONDARY)){
    float sc=Math.min(getWidth()/800f,getHeight()/450f);showGearTooltip(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);return true;
   }
   if(shopOpen&&(type==MotionEvent.ACTION_DOWN||type==MotionEvent.ACTION_BUTTON_PRESS)&&((e.getButtonState()&MotionEvent.BUTTON_SECONDARY)!=0||e.getActionButton()==MotionEvent.BUTTON_SECONDARY)){float sc=Math.min(getWidth()/800f,getHeight()/450f);showShopTooltip(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);return true;}
   if(type==MotionEvent.ACTION_DOWN&&(inventoryOpen||shopOpen)&&((e.getMetaState()&(KeyEvent.META_SHIFT_ON|KeyEvent.META_CTRL_ON))!=0)){float sc=Math.min(getWidth()/800f,getHeight()/450f);float xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;if(shopOpen)showShopTooltip(xx,yy,vw,vh);else showGearTooltip(xx,yy,vw,vh);return true;}
   if(type==MotionEvent.ACTION_UP&&(inventoryOpen||shopOpen)&&e.getEventTime()-e.getDownTime()>=450){
     float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;
     if(shopOpen)showShopTooltip(xx,yy,vw,vh);else showGearTooltip(xx,yy,vw,vh);
     return true;
    }
   if(type==MotionEvent.ACTION_DOWN&&(inventoryOpen||shopOpen))return true;
   if(type==MotionEvent.ACTION_DOWN||type==MotionEvent.ACTION_UP&&(inventoryOpen||shopOpen)){
    if(creatorOpen){float sc=Math.min(getWidth()/800f,getHeight()/450f);creatorClick(e.getX()/sc,e.getY()/sc,getWidth()/sc,getHeight()/sc);return true;}
    if(startScreen||pauseMenu){float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;if(controlsOpen){controlsBackClick(xx,yy,vw,vh);}else menuClick(xx,yy,vw,vh);return true;}
    if(shopOpen){
     float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;
     float shopLeft=vw/2-365,shopTop=vh/2-185;
     if(yy>=shopTop+51&&yy<=shopTop+82){int pages=Math.max(1,(inventory.size()+11)/12);if(xx>=shopLeft+585&&xx<=shopLeft+613){sellPage=Math.max(0,sellPage-1);return true;}if(xx>=shopLeft+670&&xx<=shopLeft+709){sellPage=Math.min(pages-1,sellPage+1);return true;}}
     int idx=shopIndex(xx,yy,vw,vh,true);
     if(idx>=0&&idx<Math.min(12,activeStock().length))trade(activeStock()[idx],true);
     else{idx=shopIndex(xx,yy,vw,vh,false);String[] names=inventoryNames();if(idx>=0&&idx<12&&sellPage*12+idx<names.length)trade(names[sellPage*12+idx],false);}
     return true;
    }
    if(inventoryOpen){updateInventoryHover(e);float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;
     int barSlot=hotbarIndexAt(xx,yy,vw,vh);
     if(barSlot>=0){inspectedGear=null;gearTooltipTime=0f;if(selectedHotbar==barSlot){hotbar[barSlot]=null;selectedHotbar=-1;}else selectedHotbar=barSlot;return true;}
     int equippedIndex=equippedSlotAt(xx,yy,vw,vh);if(equippedIndex>=0){
      String slot=new String[]{"Kopf","Waffe","Brust","Fuesse","Guertel"}[equippedIndex];
      inspectedGear=null;gearTooltipTime=0f;
      unequip(slot);return true;
     }
     int idx=inventoryIndex(xx,yy,vw,vh);if(idx>=0){String[] names=inventoryNames();if(idx<names.length){String name=names[idx];Item item=definitions.get(name);if(item!=null){inspectedGear=null;gearTooltipTime=0f;hoverX=xx;hoverY=yy;if(item.equipment())equip(name);else if(item.heal>0)assignHotbar(name);}}}return true;}
    if(calendarOpen){calendarOpen=false;return true;}
    if(hp<=0){
     float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;
     float left=vw/2-190,top=vh/2-100;
     if(xx>=left+25&&xx<=left+355){
      if(yy>=top+64&&yy<=top+112){if(hasLoadableSave()){loadGame();if(hp<=0)newCharacterAfterDeath();}else newCharacterAfterDeath();}
      else if(yy>=top+126&&yy<=top+174)newCharacterAfterDeath();
     }
     return true;
    }
    // In BlueStacks, a left mouse click arrives as a primary mouse/touch press.
    if((e.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE){
     if(e.getButtonState()==0||(e.getButtonState()&MotionEvent.BUTTON_PRIMARY)!=0)attack();
    }else attack();
   }
   return true;
  }
  boolean keyLeft=false,keyRight=false,keyUp=false,keyDown=false;
  void updateKeyboardMovement(){joyX=(keyRight?1:0)-(keyLeft?1:0);joyY=(keyDown?1:0)-(keyUp?1:0);}
  @Override public boolean onKeyDown(int key,KeyEvent e){
   if(key==KeyEvent.KEYCODE_SHIFT_LEFT||key==KeyEvent.KEYCODE_SHIFT_RIGHT){sprintHeld=true;return true;}
   if(key==KeyEvent.KEYCODE_ESCAPE||key==KeyEvent.KEYCODE_BACK){
    if(creatorOpen)return true;
    if(calendarOpen){calendarOpen=false;return true;}
    if(controlsOpen){return true;}
    if(startScreen)return true;
    if(shopOpen)shopOpen=false;else if(inventoryOpen){inventoryOpen=false;inspectedGear=null;gearTooltipTime=0f;}else pauseMenu=!pauseMenu;
    joyX=joyY=0;return true;
   }
   if(hp<=0){if(key==KeyEvent.KEYCODE_ENTER){if(hasLoadableSave()){loadGame();if(hp<=0)newCharacterAfterDeath();}else newCharacterAfterDeath();}else if(key==KeyEvent.KEYCODE_N)newCharacterAfterDeath();return true;}
   if(key==KeyEvent.KEYCODE_K&&!creatorOpen&&!startScreen&&!pauseMenu&&hp>0){calendarOpen=!calendarOpen;inventoryOpen=false;shopOpen=false;joyX=joyY=0;return true;}
   if(calendarOpen){if(key==KeyEvent.KEYCODE_ESCAPE||key==KeyEvent.KEYCODE_BACK)calendarOpen=false;return true;}
   if(creatorOpen){if(key==KeyEvent.KEYCODE_ENTER){creatorOpen=false;startScreen=false;pauseMenu=false;}return true;}
   if(startScreen||pauseMenu){if(key==KeyEvent.KEYCODE_ENTER){if(startScreen)creatorOpen=true;else pauseMenu=false;}return true;}
   switch(key){case KeyEvent.KEYCODE_A:case KeyEvent.KEYCODE_DPAD_LEFT:keyLeft=true;updateKeyboardMovement();break;
    case KeyEvent.KEYCODE_D:case KeyEvent.KEYCODE_DPAD_RIGHT:keyRight=true;updateKeyboardMovement();break;
    case KeyEvent.KEYCODE_W:case KeyEvent.KEYCODE_DPAD_UP:keyUp=true;updateKeyboardMovement();break;
    case KeyEvent.KEYCODE_S:case KeyEvent.KEYCODE_DPAD_DOWN:keyDown=true;updateKeyboardMovement();break;
    case KeyEvent.KEYCODE_SPACE:attack();break;
    case KeyEvent.KEYCODE_E:
     if(shopOpen)shopOpen=false;
     else if(indoors){
      if(!openInteriorMerchant()&&py>DOOR_IN_Y-35&&Math.abs(px-DOOR_IN_X)<25)enterOrLeave();
     }else if(Math.hypot(px-forgeX,py-(forgeY+84))<65)enterOrLeave();
     else if(Math.hypot(px-merchantX,py-merchantY)<50)openShop();
     else pickup();
     break;
    case KeyEvent.KEYCODE_I:if(shopOpen)shopOpen=false;else inventoryOpen=!inventoryOpen;inspectedGear=null;gearTooltipTime=0f;hoverSlot=-1;break;
    case KeyEvent.KEYCODE_1:useHotbar(0);break;
    case KeyEvent.KEYCODE_2:useHotbar(1);break;
    case KeyEvent.KEYCODE_3:useHotbar(2);break;
    case KeyEvent.KEYCODE_4:useHotbar(3);break;
    case KeyEvent.KEYCODE_5:useHotbar(4);break;
    case KeyEvent.KEYCODE_6:useHotbar(5);break;
    case KeyEvent.KEYCODE_7:useHotbar(6);break;
    case KeyEvent.KEYCODE_8:useHotbar(7);break;
    case KeyEvent.KEYCODE_9:useHotbar(8);break;
    case KeyEvent.KEYCODE_ENTER:break;
   }return true;
  }
  @Override public boolean onKeyUp(int key,KeyEvent e){
   if(key==KeyEvent.KEYCODE_SHIFT_LEFT||key==KeyEvent.KEYCODE_SHIFT_RIGHT){sprintHeld=false;return true;}
   if(key==KeyEvent.KEYCODE_A||key==KeyEvent.KEYCODE_DPAD_LEFT)keyLeft=false;
   if(key==KeyEvent.KEYCODE_D||key==KeyEvent.KEYCODE_DPAD_RIGHT)keyRight=false;
   if(key==KeyEvent.KEYCODE_W||key==KeyEvent.KEYCODE_DPAD_UP)keyUp=false;
   if(key==KeyEvent.KEYCODE_S||key==KeyEvent.KEYCODE_DPAD_DOWN)keyDown=false;
   updateKeyboardMovement();
   return true;
  }
 }
}
