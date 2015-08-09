package charactermanaj.model;


import java.io.File;

import junit.framework.TestCase;

public class CharacterDataFactoryTest extends TestCase {
	
	public void testLoad() throws Exception {
//		CharacterDataPersistent cf = CharacterDataPersistent.getInstance();
//		File baseDir = new File("./characters/default/character.xml");
//		CharacterData cd = cf.loadProfile(baseDir.toURL());
//		PartsDataLoader loader = new PartsDataLoader(baseDir);
//		cd.appendCharacterDataChangeListsner(new CharacterDataChangeListener() {
//			public void characterDataChange(CharacterDataChangeEvent e) {
//				System.out.println(e.getPartsIdentifier().getPartsCategory()
//						.getLocalizedCategoryName()
//						+ ":"
//						+ e.getPartsIdentifier().getLocalizedPartsName()
//						+ ":" + e.getMode());
//			}
//		});
//		cd.loadPartsData(loader);
//		System.out.println("*2nd");
//		cd.loadPartsData(loader);
	}
	
	public void testSave() throws Exception {
//		CharacterDataPersistent cf = CharacterDataPersistent.getInstance();
//		CharacterData cd = cf.load(new File("./characters/default2/character.xml").toURL());
//		
//		cf.save(cd, new File("./characters/default2"));
	}
	
	public void test1() throws Exception {
		File d = new File("a").getAbsoluteFile();
		System.out.println(d);
		File f = new File(d, "b/c/d").getCanonicalFile();
		System.out.println(f.isAbsolute() + ":" + f);
	}
	
}
