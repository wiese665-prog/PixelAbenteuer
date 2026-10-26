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
  String name,rarity,slot;int dmg,armor,hp,heal;
  Item(String n,String rar,String sl,int d,int a,int h,int healing){name=n;rarity=rar;slot=sl;dmg=d;armor=a;hp=h;heal=healing;}
  boolean equipment(){return !slot.equals("");}
 }
 class Game extends View {
  final int WORLD_W=1600,WORLD_H=1000;
  Canvas canvas;Paint paint=new Paint();Random random=new Random(42);
  ArrayList<Mob> mobs=new ArrayList<>();ArrayList<Decoration> decorations=new ArrayList<>();ArrayList<Drop> drops=new ArrayList<>();LinkedHashMap<String,Integer> inventory=new LinkedHashMap<>();HashMap<String,Bitmap> itemImages=new HashMap<>();
  HashMap<String,Item> definitions=new HashMap<>();HashMap<String,String> equipped=new HashMap<>();
  String[] hotbar=new String[5];int copper=0,silver=0,gold=0,hoverSlot=-1;float hoverX=0,hoverY=0;
  boolean inventoryOpen=false;String pickupNotice="";float noticeTime=0;
  HashMap<String,Bitmap> sprites=new HashMap<>();
  float px=180,py=180,joyX=0,joyY=0,elapsed=0,attackAnim=0,hurtAnim=0,deathAnim=0,attackCooldown=0,playerHealthVisible=0;
  int hp=12,maxHp=12,kills=0,animalKills=0,playerDir=0,playerLevel=1,exp=0,attackPower=3,lastPlayerDamage=0;float playerDamageTime=0;float swordTimer=0;boolean sword=false;
  long previous=System.nanoTime();
  Game(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);paint.setFilterBitmap(false);loadSprites();loadItems();loadDecorations();spawnMobs();}
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
   for(int i=0;i<19;i++){float x=300+(i*251)%1200,y=90+(i*197)%810;Bitmap b=ruins.isEmpty()?null:ruins.get(i%ruins.size());decorations.add(new Decoration(b,b,x,y,94,90,true));}
   for(int i=0;i<85;i++){float x=60+(i*167)%1480,y=65+(i*283)%870;if(Math.hypot(x-180,y-180)<140)continue;Bitmap b=rocks.isEmpty()?null:rocks.get(i%rocks.size());decorations.add(new Decoration(b,rockMasks.isEmpty()?b:rockMasks.get(i%rockMasks.size()),x,y,20+(i%3)*4,19+(i%4)*4,true));}
   for(int i=0;i<70&&!plants.isEmpty();i++){float x=30+(i*131)%1540,y=40+(i*223)%920;decorations.add(new Decoration(plants.get(i%plants.size()),plants.get(i%plants.size()),x,y,34+(i%3)*12,40+(i%3)*10,true));}
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
   for(Decoration d:decorations)if(d.solid&&collides(d,x,y))return true;
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
  void define(String name,String rarity,String slot,int dmg,int armor,int bonusHp,int heal){
   definitions.put(name,new Item(name,rarity,slot,dmg,armor,bonusHp,heal));
  }
  void loadItems(){
   String[] filenames={"Healthpotion.png","Healthpotion large.png","Healpotion med.png","Green Apple.png","Ham.png","Apple.png","Mushroom.png","Cheese.png","Bag.png","Belt.png","Helm.png","Iron Armor.png","Iron Boot.png","Iron Helmet.png","Leather Armor.png","Leather Boot.png","Leather Helmet.png","Wizard Hat.png","Wooden Armor.png","Wood Log.png","Wool.png","Copper Coin.png","Gear.png","Golden Coin.png","Silver Coin.png","Rune Stone.png","Map.png","Bone.png","Egg.png","Feather.png","Monster Egg.png","Monster Eye.png","Monster Meat.png","Skull.png","Slime Gel.png","Coal.png","Copper Ingot.png","Copper Nugget.png","Crystal.png","Cut Emerald.png","Cut Ruby.png","Cut Sapphire.png","Cut Topaz.png","Diamond.png","Emerald.png","Gold Nugget.png","Golden Ingot.png","Obsidian.png","Pearl.png","Ruby.png","Sapphire.png","Silver Ingot.png","Silver Nugget.png","Topaz.png","Iron Sword.png","Silver Sword.png","Wooden Sword.png","Golden Sword.png"};
   for(String filename:filenames){Bitmap b=asset(filename);if(b!=null)itemImages.put(filename.substring(0,filename.length()-4),b);}
   define("Wooden Sword","Gewoehnlich","Waffe",1,0,0,0);
   define("Iron Sword","Ungewoehnlich","Waffe",3,0,0,0);
   define("Silver Sword","Selten","Waffe",5,0,0,0);
   define("Golden Sword","Episch","Waffe",8,0,0,0);
   define("Wooden Armor","Gewoehnlich","Brust",0,1,1,0);
   define("Leather Armor","Ungewoehnlich","Brust",0,2,3,0);
   define("Iron Armor","Selten","Brust",0,4,5,0);
   define("Leather Helmet","Gewoehnlich","Kopf",0,1,1,0);
   define("Iron Helmet","Selten","Kopf",0,3,3,0);
   define("Wizard Hat","Episch","Kopf",2,1,4,0);
   define("Leather Boot","Gewoehnlich","Fuesse",0,1,0,0);
   define("Iron Boot","Selten","Fuesse",0,3,2,0);
   define("Belt","Ungewoehnlich","Guertel",0,1,2,0);
   define("Helm","Ungewoehnlich","Kopf",0,2,2,0);
   define("Bag","Gewoehnlich","Guertel",0,0,2,0);
   define("Apple","Gewoehnlich","",0,0,0,3);
   define("Green Apple","Gewoehnlich","",0,0,0,4);
   define("Mushroom","Gewoehnlich","",0,0,0,2);
   define("Cheese","Gewoehnlich","",0,0,0,5);
   define("Ham","Ungewoehnlich","",0,0,0,8);
   define("Monster Meat","Ungewoehnlich","",0,0,0,6);
   define("Healthpotion","Ungewoehnlich","",0,0,0,10);
   define("Healpotion med","Selten","",0,0,0,20);
   define("Healthpotion large","Episch","",0,0,0,40);
  }
  void itemIcon(String item,float x,float y,float scale){
   Bitmap b=itemImages.get(item);
   if(b==null)return;
   paint.setColor(Color.WHITE);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,null,new RectF(x,y,x+24*scale,y+24*scale),paint);
  }
  int armorValue(){int total=0;for(String name:equipped.values()){Item i=definitions.get(name);if(i!=null)total+=i.armor;}return total;}
  int bonusHealth(){int total=0;for(String name:equipped.values()){Item i=definitions.get(name);if(i!=null)total+=i.hp;}return total;}
  void recalcStats(){
   int oldMax=maxHp;maxHp=12+(playerLevel-1)*4+bonusHealth();
   hp=Math.min(maxHp,hp+Math.max(0,maxHp-oldMax));
   attackPower=3+(playerLevel-1);
   Item weapon=definitions.get(equipped.get("Waffe"));if(weapon!=null)attackPower+=weapon.dmg;
   for(String name:equipped.values()){Item i=definitions.get(name);if(i!=null&&!i.slot.equals("Waffe"))attackPower+=i.dmg;}
  }
  void equip(String name){
   Item item=definitions.get(name);if(item==null||!item.equipment()||inventory.getOrDefault(name,0)<=0)return;
   String old=equipped.get(item.slot);if(name.equals(old))return;
   inventory.put(name,inventory.get(name)-1);if(inventory.get(name)<=0)inventory.remove(name);
   if(old!=null)inventory.put(old,inventory.getOrDefault(old,0)+1);
   equipped.put(item.slot,name);recalcStats();
   pickupNotice=name+" angelegt";noticeTime=2f;
  }
  void useItem(String name){
   Item item=definitions.get(name);
   if(item==null||item.heal<=0||inventory.getOrDefault(name,0)<=0||hp<=0||hp>=maxHp)return;
   hp=Math.min(maxHp,hp+item.heal);inventory.put(name,inventory.get(name)-1);
   if(inventory.get(name)<=0)inventory.remove(name);
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
   if(inventoryOpen)return;
   Drop d=nearestDrop();if(d==null)return;
   inventory.put(d.item,inventory.containsKey(d.item)?inventory.get(d.item)+1:1);
   pickupNotice="+1 "+d.item;noticeTime=1.8f;drops.remove(d);
  }
  String[] inventoryNames(){return inventory.keySet().toArray(new String[0]);}
  void drawInventory(float vw,float vh){
   float left=vw/2-290,top=vh/2-193;
   drawRect(col("#a78b61"),left-4,top-4,588,388);
   drawRect(col("#222b32"),left,top,580,380);
   paint.setColor(col("#f5deb0"));paint.setTextSize(23);paint.setTypeface(Typeface.DEFAULT_BOLD);
   canvas.drawText("INVENTAR",left+18,top+32,paint);
   paint.setTextSize(12);canvas.drawText("I: Schliessen   Klick: Anlegen   1-5: Hotbar",left+200,top+30,paint);
   String[] names=inventoryNames();
   for(int i=0;i<Math.min(24,names.length);i++){
    float x=left+16+(i%8)*69,y=top+46+(i/8)*75;
    drawRect(col("#121b20"),x,y,63,68);drawRect(col("#48545b"),x+3,y+3,57,62);
    itemIcon(names[i],x+13,y+4,1.5f);
    paint.setColor(Color.WHITE);paint.setTextSize(9);
    String label=names[i];if(label.length()>11)label=label.substring(0,10)+".";
    canvas.drawText(label,x+3,y+54,paint);
    paint.setColor(col("#ffe1a0"));paint.setTextSize(11);
    canvas.drawText("x"+inventory.get(names[i]),x+44,y+13,paint);
   }
   paint.setColor(col("#f1d7a4"));paint.setTextSize(13);
   canvas.drawText("Ausgeruestet:",left+17,top+293,paint);
   int i=0;for(String slot:new String[]{"Waffe","Kopf","Brust","Fuesse","Guertel"}){
    String name=equipped.get(slot);paint.setTextSize(11);
    canvas.drawText(slot+": "+(name==null?"-":name),left+17+(i%3)*186,top+315+(i/3)*20,paint);i++;
   }
   if(hoverSlot>=0&&hoverSlot<names.length){
    Item item=definitions.get(names[hoverSlot]);
    if(item!=null){
     float tx=Math.min(vw-214,hoverX+14),ty=Math.min(vh-118,hoverY+10);
     drawRect(col("#10171e"),tx,ty,208,105);drawRect(col("#6d6b5d"),tx+2,ty+2,204,101);
     drawRect(col("#202a33"),tx+4,ty+4,200,97);
     paint.setTextSize(13);paint.setColor(col("#fff1c9"));canvas.drawText(item.name,tx+10,ty+20,paint);
     paint.setTextSize(12);paint.setColor(item.rarity.equals("Episch")?col("#d69af4"):item.rarity.equals("Selten")?col("#76b7f7"):col("#d6e3d0"));
     canvas.drawText(item.rarity,tx+10,ty+38,paint);paint.setColor(Color.WHITE);
     canvas.drawText(item.equipment()?"Slot: "+item.slot:"Verbrauchbar",tx+10,ty+55,paint);
     canvas.drawText(item.equipment()?"DMG +"+item.dmg+"  DEF +"+item.armor+"  HP +"+item.hp:"Heilung: +"+item.heal+" HP",tx+10,ty+75,paint);
     canvas.drawText(item.equipment()?"Klicken: Anlegen":item.heal>0?"Klicken: Hotbar zuweisen":"Material",tx+10,ty+92,paint);
    }
   }
   paint.setTypeface(Typeface.DEFAULT);
  }
  void drawHotbar(float vw,float vh){
   for(int i=0;i<5;i++){
    float x=vw/2-128+i*53,y=vh-53;
    drawRect(col("#141c23"),x-2,y-2,50,48);drawRect(col("#4d5b62"),x,y,46,44);
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
   paint.setColor(Color.WHITE);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,new Rect(frame*frameWidth,0,(frame+1)*frameWidth,b.getHeight()),new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);
  }
  void update(float dt){
   elapsed+=dt;noticeTime=Math.max(0,noticeTime-dt);swordTimer=Math.max(0,swordTimer-dt);sword=swordTimer>0;attackCooldown=Math.max(0,attackCooldown-dt);attackAnim=Math.max(0,attackAnim-dt);hurtAnim=Math.max(0,hurtAnim-dt);playerHealthVisible=Math.max(0,playerHealthVisible-dt);playerDamageTime=Math.max(0,playerDamageTime-dt);
   if(hp<=0){deathAnim+=dt;return;}if(inventoryOpen)return;
   float mx=joyX,my=joyY,length=(float)Math.hypot(mx,my);
   if(length>1){mx/=length;my/=length;}
   if(Math.abs(mx)+Math.abs(my)>.08f){
    playerDir=Math.abs(mx)>Math.abs(my)?(mx<0?2:3):(my<0?1:0);
    float step=140*dt/3f;
    for(int sub=0;sub<3;sub++){
     float nx=px+mx*step,ny=py+my*step;
     if(!blocked(nx,py))px=nx;
     if(!blocked(px,ny))py=ny;
    }
   }
   for(Mob m:mobs){
    if(m.dead){m.deathTimer+=dt;continue;}
    m.anim+=dt;m.attackTimer=Math.max(0,m.attackTimer-dt);m.hurtTimer=Math.max(0,m.hurtTimer-dt);m.healthVisible=Math.max(0,m.healthVisible-dt);m.damageNumberTime=Math.max(0,m.damageNumberTime-dt);
    float distance=(float)Math.hypot(px-m.x,py-m.y);
    float vx=0,vy=0;String action="idle";
    if(m.hurtTimer>0){action="hurt";}
    else if(m.hostile()&&distance<330){
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
    if(Math.abs(vx)+Math.abs(vy)>1){
     float step=dt/3f;
     for(int sub=0;sub<3;sub++){
      float nx=m.x+vx*step,ny=m.y+vy*step;
      if(!blocked(nx,m.y))m.x=nx;else m.decision=0;
      if(!blocked(m.x,ny))m.y=ny;else m.decision=0;
     }
     m.dir=direction(vx,vy);
    }
    if(!m.action.equals(action)){m.action=action;m.anim=0;}
   }
  }
  int direction(float vx,float vy){return Math.abs(vx)>Math.abs(vy)?(vx<0?2:3):(vy<0?1:0);}
  void attack(){
   if(hp<=0||attackCooldown>0||inventoryOpen)return;
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
   playerLevel=1;exp=0;attackPower=3;maxHp=12;hp=maxHp;equipped.clear();copper=silver=gold=0;java.util.Arrays.fill(hotbar,null);playerDamageTime=0;px=180;py=180;joyX=joyY=0;deathAnim=0;swordTimer=0;sword=false;attackAnim=0;playerHealthVisible=0;kills=animalKills=0;mobs.clear();drops.clear();inventory.clear();inventoryOpen=false;spawnMobs();
  }
  @Override protected void onDraw(Canvas c){
   canvas=c;float dt=Math.min(.05f,(System.nanoTime()-previous)/1000000000f);previous=System.nanoTime();update(dt);
   float w=getWidth(),h=getHeight(),scale=Math.min(w/800f,h/450f),vw=w/scale,vh=h/scale;
   paint.reset();paint.setAntiAlias(false);paint.setFilterBitmap(false);paint.setColor(Color.WHITE);
   c.drawColor(col("#407d46"));c.save();c.scale(scale,scale);c.save();c.translate(vw/2-px,vh/2-py);
   int startX=(int)Math.floor((px-vw/2)/32f)*32-32,endX=(int)Math.ceil((px+vw/2)/32f)*32+32;
   int startY=(int)Math.floor((py-vh/2)/32f)*32-32,endY=(int)Math.ceil((py+vh/2)/32f)*32+32;
   for(int x=startX;x<endX;x+=32)for(int y=startY;y<endY;y+=32){
    int n=(x*37+y*19)%7;drawRect(n<3?col("#407d46"):col("#47884a"),x,y,32,32);
    if(n==1)drawRect(col("#6a9a53"),x+8,y+7,5,4);
   }
   for(Decoration d:decorations){
    if(d.image!=null){paint.setColor(Color.WHITE);c.drawBitmap(d.image,null,new RectF(d.x-d.w/2,d.y-d.h*.8f,d.x+d.w/2,d.y+d.h*.2f),paint);}
    else drawRect(col("#82776b"),d.x-d.w/2,d.y-d.h/2,d.w,d.h);
   }
   for(Drop d:drops){float bob=(float)Math.sin(elapsed*3+d.bob)*2;
    drawRect(col("#28342b"),d.x-11,d.y+7,22,5);itemIcon(d.item,d.x-12,d.y-17+bob,1f);
   }
   for(Mob m:mobs){
    if(m.dead&&m.deathTimer>2.5f)continue;
    String action=m.dead?"death":m.hurtTimer>0?"hurt":m.action;
    float size=m.hostile()?65:m.kind.equals("boar")?45:m.kind.equals("deer")?43:36;
    drawSprite(m.kind,action,m.dir,m.x,m.y,size,size,m.dead?m.deathTimer:m.anim,m.dead&&m.deathTimer>.7f);
    if(!m.dead){paint.setColor(Color.WHITE);paint.setTextSize(12);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("Lv. "+m.level,m.x,m.y-size*.64f,paint);paint.setTextAlign(Paint.Align.LEFT);}
    if(m.healthVisible>0&&(!m.dead||m.deathTimer<1f))healthBar(m.x,m.y-size*.65f-12,42,m.hp,m.maxHp);
    damageText(m.x,m.y-size*.65f-22,m.lastDamage,m.damageNumberTime);
   }
   String playerKind=sword?"player_sword":"player_unarmed";
   String playerAction=hp<=0?"death":hurtAnim>0?"hurt":attackAnim>0&&sword?"attack":Math.abs(joyX)+Math.abs(joyY)>.08f?"run":"idle";
   drawSprite(playerKind,playerAction,playerDir,px,py,80,80,hp<=0?deathAnim:attackAnim>0?.4f-attackAnim:elapsed,hp<=0&&deathAnim>.8f);
   if(playerHealthVisible>0)healthBar(px,py-56,52,hp,maxHp);
   damageText(px,py-66,lastPlayerDamage,playerDamageTime);
   Drop nearby=nearestDrop();
   if(nearby!=null&&!inventoryOpen){paint.setColor(col("#fff0c5"));paint.setTextSize(13);paint.setTextAlign(Paint.Align.CENTER);
    canvas.drawText("E  "+nearby.item+" aufheben",px,py-60,paint);paint.setTextAlign(Paint.Align.LEFT);}
   c.restore();
   drawRect(col("#142b24"),0,0,vw,37);paint.setColor(Color.WHITE);paint.setTextSize(18);
   c.drawText("Lv. "+playerLevel+" EXP "+exp+"/"+expNeeded()+" DMG "+attackPower+" DEF "+armorValue(),12,25,paint);
   itemIcon("Copper Coin",vw-202,7,.85f);itemIcon("Silver Coin",vw-137,7,.85f);itemIcon("Golden Coin",vw-72,7,.85f);
   paint.setColor(Color.WHITE);paint.setTextSize(15);c.drawText(""+copper,vw-178,25,paint);c.drawText(""+silver,vw-113,25,paint);c.drawText(""+gold,vw-48,25,paint);
   drawHotbar(vw,vh);
   if(noticeTime>0){paint.setColor(col("#f7e0a6"));paint.setTextSize(17);c.drawText(pickupNotice,12,60,paint);}
   if(inventoryOpen)drawInventory(vw,vh);
   if(hp<=0){drawRect(col("#111111"),vw/2-180,vh/2-45,360,90);paint.setColor(Color.WHITE);paint.setTextSize(24);c.drawText("GAME OVER - Tippen",vw/2-145,vh/2+7,paint);}
   c.restore();postInvalidateDelayed(16);
  }
  int inventoryIndex(float x,float y,float vw,float vh){
   float left=vw/2-290,top=vh/2-193;
   float dx=x-left-16,dy=y-top-46;
   if(dx<0||dy<0||dx>=8*69||dy>=3*75)return -1;
   int col=(int)(dx/69),row=(int)(dy/75);
   if(dx%69>=63||dy%75>=68)return -1;
   return row*8+col;
  }
  @Override public boolean onHoverEvent(MotionEvent e){
   if(inventoryOpen){float sc=Math.min(getWidth()/800f,getHeight()/450f),vw=getWidth()/sc,vh=getHeight()/sc;
    hoverX=e.getX()/sc;hoverY=e.getY()/sc;hoverSlot=inventoryIndex(hoverX,hoverY,vw,vh);invalidate();return true;}
   return super.onHoverEvent(e);
  }
  @Override public boolean onTouchEvent(MotionEvent e){
   int type=e.getActionMasked();
   if(type==MotionEvent.ACTION_DOWN){
    if(inventoryOpen){float sc=Math.min(getWidth()/800f,getHeight()/450f),xx=e.getX()/sc,yy=e.getY()/sc,vw=getWidth()/sc,vh=getHeight()/sc;
     int idx=inventoryIndex(xx,yy,vw,vh);if(idx>=0){String[] names=inventoryNames();if(idx<names.length){String name=names[idx];Item item=definitions.get(name);if(item!=null){if(item.equipment())equip(name);else if(item.heal>0){int slot=0;while(slot<4&&hotbar[slot]!=null)slot++;hotbar[slot]=name;pickupNotice=name+" auf Hotbar "+(slot+1);noticeTime=2f;}}}}return true;}
    if(hp<=0){restart();return true;}
    // In BlueStacks, a left mouse click arrives as a primary mouse/touch press.
    if((e.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE){
     if(e.getButtonState()==0||(e.getButtonState()&MotionEvent.BUTTON_PRIMARY)!=0)attack();
    }else attack();
   }
   return true;
  }
  @Override public boolean onGenericMotionEvent(MotionEvent e){
   if((e.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE&&e.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS
      &&e.getActionButton()==MotionEvent.BUTTON_PRIMARY){attack();return true;}
   return super.onGenericMotionEvent(e);
  }
  @Override public boolean onKeyDown(int key,KeyEvent e){
   switch(key){case KeyEvent.KEYCODE_A:case KeyEvent.KEYCODE_DPAD_LEFT:joyX=-1;break;
    case KeyEvent.KEYCODE_D:case KeyEvent.KEYCODE_DPAD_RIGHT:joyX=1;break;
    case KeyEvent.KEYCODE_W:case KeyEvent.KEYCODE_DPAD_UP:joyY=-1;break;
    case KeyEvent.KEYCODE_S:case KeyEvent.KEYCODE_DPAD_DOWN:joyY=1;break;
    case KeyEvent.KEYCODE_SPACE:attack();break;
    case KeyEvent.KEYCODE_E:pickup();break;
    case KeyEvent.KEYCODE_I:inventoryOpen=!inventoryOpen;hoverSlot=-1;break;
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
