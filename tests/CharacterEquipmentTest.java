package de.pixelabenteuer.game;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public final class CharacterEquipmentTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static BufferedImage sheet(String page,String layer,String type)throws Exception{
  Path p=Path.of("app/src/main/assets/sprites/ManaSeedCharacter/char_a_"+page+"/"+layer+"/char_a_"+page+"_"+layer+"_"+type+"_v01.png");
  return ImageIO.read(p.toFile());
 }
 public static void main(String[] args)throws Exception{
  String[][] cases={{"Wooden Helmet","5hat","wdHl"},{"Wooden Armor","1out","wdAr"},{"Leather Armor","1out","ltAr"},{"Iron Armor","1out","irAr"},{"Leather Helmet","5hat","leHl"},{"Iron Helmet","5hat","irHl"},{"Helm","5hat","stHl"},{"Wizard Hat","5hat","mgHt"},{"Magierhut","5hat","mgHt"},{"Waldlaeuferkapuze","5hat","hood"},{"Magierrobe","1out","robe"},{"Waldlaeuferwams","1out","rngr"},{"Leather Boot","8feet","leBt"},{"Iron Boot","8feet","irBt"},{"Belt","9belt","belt"},{"Bag","9belt","bag"}};
  int resolved=0;
  for(String[] item:cases)for(String suffix:new String[]{""," [Rare]"," [Legendär] {42}"}){
   String raw=item[0]+suffix,actual=item[1].equals("1out")?CharacterEquipment.outfit(raw,0):item[1].equals("5hat")?CharacterEquipment.hat(raw):item[1].equals("8feet")?CharacterEquipment.boots(raw):CharacterEquipment.belt(raw);
   check(actual.equals(item[2]),"wrong equipped visual: "+raw+" => "+actual);
   for(String page:new String[]{"p1","pONE2","pONE3","pBOW","pCAST"}){
    BufferedImage im=sheet(page,item[1],actual);check(im.getWidth()==512&&im.getHeight()==512,"wrong sheet dimensions");
    for(int row=0;row<4;row++){
     boolean visible=false;for(int y=row*64;y<(row+1)*64;y++)for(int x=0;x<64;x++)if((im.getRGB(x,y)>>>24)>0)visible=true;
     check(visible,"missing equipped idle/draw direction "+raw+" "+page+" "+row);
    }
    resolved++;
   }
  }
  check(CharacterEquipment.outfit(null,0).equals("boxr"),"empty male outfit");
  check(CharacterEquipment.outfit(null,1).equals("undi"),"empty female outfit");
  check(CharacterEquipment.hat(null).isEmpty()&&CharacterEquipment.boots(null).isEmpty()&&CharacterEquipment.belt(null).isEmpty(),"unequip left visible gear");
  System.out.println(resolved+" actual item/rarity/page resolutions passed; all 16 items, four directions and empty slots.");
 }
}
