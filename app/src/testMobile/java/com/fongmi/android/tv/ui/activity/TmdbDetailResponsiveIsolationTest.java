package com.fongmi.android.tv.ui.activity;
import org.junit.Test;
import static org.junit.Assert.*;
public class TmdbDetailResponsiveIsolationTest {
    @Test public void mobileFlavorCannotEnterTvLayout(){assertFalse(com.fongmi.android.tv.utils.Util.isLeanback());assertTrue(com.fongmi.android.tv.utils.Util.isMobile());}
    @Test public void sharedXmlStillKeepsOriginalMobileActionsAndPlayerGeometry()throws Exception{
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        var document=factory.newDocumentBuilder().parse(java.nio.file.Path.of("src/main/res/layout/activity_tmdb_detail.xml").toFile());
        var all=document.getElementsByTagName("*");String ns="http://schemas.android.com/apk/res/android";org.w3c.dom.Element play=null,actions=null,panel=null;
        for(int i=0;i<all.getLength();i++){var e=(org.w3c.dom.Element)all.item(i);String id=e.getAttributeNS(ns,"id");if(id.equals("@+id/play"))play=e;if(id.equals("@+id/detailActions"))actions=e;if(id.equals("@+id/playerPanel"))panel=e;}
        assertNotNull(play);assertNotNull(actions);assertNotNull(panel);assertEquals("match_parent",play.getAttributeNS(ns,"layout_width"));assertEquals("vertical",actions.getAttributeNS(ns,"orientation"));assertEquals("252dp",panel.getAttributeNS(ns,"layout_height"));assertEquals("match_parent",panel.getAttributeNS(ns,"layout_width"));
    }
}
