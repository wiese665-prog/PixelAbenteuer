package de.pixelabenteuer.game;

/** One shared visual resolver for inventory, creator and every world pose. */
final class CharacterEquipment {
 private CharacterEquipment() {}
 static String base(String name) {
  if(name==null)return "";
  int unique=name.lastIndexOf(" {");if(unique>=0&&name.endsWith("}"))name=name.substring(0,unique);
  int rank=name.lastIndexOf(" [");return rank>=0&&name.endsWith("]")?name.substring(0,rank):name;
 }
 static String outfit(String item,int gender) {
  switch(base(item)) {
   case "Wooden Armor":return "wdAr";
   case "Leather Armor":return "ltAr";
   case "Iron Armor":return "irAr";
   case "Waldlaeuferwams":return "rngr";
   case "Magierrobe":return "robe";
   case "":return gender==0?"boxr":"undi";
   default:return "fstr";
  }
 }
 static String hat(String item) {
  switch(base(item)) {
   case "Wooden Helmet":return "wdHl";
   case "Leather Helmet":return "leHl";
   case "Iron Helmet":return "irHl";
   case "Helm":return "stHl";
   case "Wizard Hat":case "Magierhut":return "mgHt";
   case "Waldlaeuferkapuze":return "hood";
   case "":return "";
   default:return "pfht";
  }
 }
 static String boots(String item) {
  switch(base(item)) {case "Leather Boot":return "leBt";case "Iron Boot":return "irBt";default:return "";}
 }
 static String belt(String item) {
  switch(base(item)) {case "Belt":return "belt";case "Bag":return "bag";default:return "";}
 }
}
