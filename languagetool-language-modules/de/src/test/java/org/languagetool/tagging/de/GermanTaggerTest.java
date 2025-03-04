/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.tagging.de;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.WordData;
import org.junit.Test;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@SuppressWarnings("ConstantConditions")
public class GermanTaggerTest {

  private final GermanTagger tagger = new GermanTagger();

  @Test
  public void testLemmaOfForDashCompounds() throws IOException {
    AnalyzedTokenReadings aToken = tagger.lookup("Zahn-Arzt-Verband");
    List<String> lemmas = new ArrayList<>();
    for (AnalyzedToken analyzedToken : aToken) {
      lemmas.add(analyzedToken.getLemma());
    }
    assertTrue(lemmas.contains("Zahnarztverband"));
  }
  
  @Test
  public void testGenderGap() throws IOException {
    // https://github.com/languagetool-org/languagetool/issues/2417
    assertTrue(tagger.tag(Arrays.asList("viele", "Freund", "*", "innen")).get(1).hasPartialPosTag(":PLU:FEM"));
    assertTrue(tagger.tag(Arrays.asList("viele", "Freund", "_", "innen")).get(1).hasPartialPosTag(":PLU:FEM"));
    assertTrue(tagger.tag(Arrays.asList("viele", "Freund", ":", "innen")).get(1).hasPartialPosTag(":PLU:FEM"));
    assertTrue(tagger.tag(Arrays.asList("viele", "Freund", "/", "innen")).get(1).hasPartialPosTag(":PLU:FEM"));
    assertTrue(tagger.tag(Arrays.asList("jede", "*", "r", "Mitarbeiter", "*", "in")).get(0).hasPartialPosTag("PRO:IND:NOM:SIN:FEM"));
    assertTrue(tagger.tag(Arrays.asList("jede", "*", "r", "Mitarbeiter", "*", "in")).get(0).hasPartialPosTag("PRO:IND:NOM:SIN:MAS"));
    assertTrue(tagger.tag(Arrays.asList("jede", "*", "r", "Mitarbeiter", "*", "in")).get(3).hasPartialPosTag("SUB:NOM:SIN:FEM"));
    assertTrue(tagger.tag(Arrays.asList("jede", "*", "r", "Mitarbeiter", "*", "in")).get(3).hasPartialPosTag("SUB:NOM:SIN:MAS"));
  }
  
  @Test
  public void testIgnoreDomain() throws IOException {
    List<AnalyzedTokenReadings> aToken = tagger.tag(Arrays.asList("bundestag", ".", "de"));
    assertFalse(aToken.get(0).isTagged());
  }

  @Test
  public void testIgnoreImperative() throws IOException {
    List<AnalyzedTokenReadings> aToken = tagger.tag(Arrays.asList("zehnfach"));
    assertFalse(aToken.get(0).isTagged());
  }

  @Test
  public void testTagger() throws IOException {
    AnalyzedTokenReadings aToken = tagger.lookup("Haus");
    assertEquals("Haus[Haus/SUB:AKK:SIN:NEU, Haus/SUB:DAT:SIN:NEU, Haus/SUB:NOM:SIN:NEU]", toSortedString(aToken));
    assertEquals("Haus", aToken.getReadings().get(0).getLemma());
    assertEquals("Haus", aToken.getReadings().get(1).getLemma());
    assertEquals("Haus", aToken.getReadings().get(2).getLemma());

    AnalyzedTokenReadings aToken2 = tagger.lookup("Hauses");
    assertEquals("Hauses[Haus/SUB:GEN:SIN:NEU]", toSortedString(aToken2));
    assertEquals("Haus", aToken2.getReadings().get(0).getLemma());

    assertNull(tagger.lookup("hauses"));
    assertNull(tagger.lookup("Groß"));

    assertEquals("Lieblingsbuchstabe[Lieblingsbuchstabe/SUB:NOM:SIN:MAS]", toSortedString(tagger.lookup("Lieblingsbuchstabe")));

    AnalyzedTokenReadings aToken3 = tagger.lookup("großer");
    assertEquals("großer[groß/ADJ:DAT:SIN:FEM:GRU:SOL, groß/ADJ:GEN:PLU:FEM:GRU:SOL, groß/ADJ:GEN:PLU:MAS:GRU:SOL, " +
            "groß/ADJ:GEN:PLU:NEU:GRU:SOL, groß/ADJ:GEN:SIN:FEM:GRU:SOL, groß/ADJ:NOM:SIN:MAS:GRU:IND, " +
            "groß/ADJ:NOM:SIN:MAS:GRU:SOL]", toSortedString(tagger.lookup("großer")));
    assertEquals("groß", aToken3.getReadings().get(0).getLemma());

    // checks for github issue #635: Some German verbs on the beginning of a sentences are identified only as substantive
    assertTrue(tagger.tag(Collections.singletonList("Haben"), true).toString().contains("VER"));
    assertTrue(tagger.tag(Collections.singletonList("Können"), true).toString().contains("VER"));
    assertTrue(tagger.tag(Collections.singletonList("Gerade"), true).toString().contains("ADJ"));

    // from both german.dict and added.txt:
    AnalyzedTokenReadings aToken4 = tagger.lookup("Interessen");
    assertEquals("Interessen[Interesse/SUB:AKK:PLU:NEU, Interesse/SUB:DAT:PLU:NEU, " +
                    "Interesse/SUB:GEN:PLU:NEU, Interesse/SUB:NOM:PLU:NEU]",
            toSortedString(aToken4));
    assertEquals("Interesse", aToken4.getReadings().get(0).getLemma());
    assertEquals("Interesse", aToken4.getReadings().get(1).getLemma());
    assertEquals("Interesse", aToken4.getReadings().get(2).getLemma());
    assertEquals("Interesse", aToken4.getReadings().get(3).getLemma());

    // words that are not in the dictionary but that are recognized thanks to noun splitting:
    AnalyzedTokenReadings aToken5 = tagger.lookup("Donaudampfschiff");
    assertEquals("Donaudampfschiff[Donaudampfschiff/SUB:AKK:SIN:NEU, Donaudampfschiff/SUB:DAT:SIN:NEU, " +
            "Donaudampfschiff/SUB:NOM:SIN:NEU]", toSortedString(aToken5));
    assertEquals("Donaudampfschiff", aToken5.getReadings().get(0).getLemma());
    assertEquals("Donaudampfschiff", aToken5.getReadings().get(1).getLemma());

    AnalyzedTokenReadings aToken6 = tagger.lookup("Häuserkämpfe");
    assertEquals("Häuserkämpfe[Häuserkampf/SUB:AKK:PLU:MAS, Häuserkampf/SUB:GEN:PLU:MAS, Häuserkampf/SUB:NOM:PLU:MAS]",
            toSortedString(aToken6));
    assertEquals("Häuserkampf", aToken6.getReadings().get(0).getLemma());
    assertEquals("Häuserkampf", aToken6.getReadings().get(1).getLemma());
    assertEquals("Häuserkampf", aToken6.getReadings().get(2).getLemma());

    AnalyzedTokenReadings aToken7 = tagger.lookup("Häuserkampfes");
    assertEquals("Häuserkampfes[Häuserkampf/SUB:GEN:SIN:MAS]", toSortedString(aToken7));
    assertEquals("Häuserkampf", aToken7.getReadings().get(0).getLemma());

    AnalyzedTokenReadings aToken8 = tagger.lookup("Häuserkampfs");
    assertEquals("Häuserkampfs[Häuserkampf/SUB:GEN:SIN:MAS]", toSortedString(aToken8));
    assertEquals("Häuserkampf", aToken8.getReadings().get(0).getLemma());

    AnalyzedTokenReadings aToken9 = tagger.lookup("Lieblingsfarben");
    assertEquals("Lieblingsfarben[Lieblingsfarbe/SUB:AKK:PLU:FEM, Lieblingsfarbe/SUB:DAT:PLU:FEM, " +
            "Lieblingsfarbe/SUB:GEN:PLU:FEM, Lieblingsfarbe/SUB:NOM:PLU:FEM]", toSortedString(aToken9));
    assertEquals("Lieblingsfarbe", aToken9.getReadings().get(0).getLemma());

    AnalyzedTokenReadings aToken10 = tagger.lookup("Autolieblingsfarben");
    assertEquals("Autolieblingsfarben[Autolieblingsfarbe/SUB:AKK:PLU:FEM, Autolieblingsfarbe/SUB:DAT:PLU:FEM, " +
            "Autolieblingsfarbe/SUB:GEN:PLU:FEM, Autolieblingsfarbe/SUB:NOM:PLU:FEM]", toSortedString(aToken10));
    assertEquals("Autolieblingsfarbe", aToken10.getReadings().get(0).getLemma());

    AnalyzedTokenReadings aToken11 = tagger.lookup("übrigbleibst");
    assertEquals("übrigbleibst[übrigbleiben/VER:2:SIN:PRÄ:NON:NEB]", toSortedString(aToken11));
    assertEquals("übrigbleiben", aToken11.getReadings().get(0).getLemma());

    AnalyzedTokenReadings aToken12 = tagger.lookup("IT-Dienstleistungsunternehmen");
    assertTrue(aToken12.getReadings().get(0).getPOSTag().matches("SUB.*"));
    assertEquals("IT-Dienstleistungsunternehmen", aToken12.getReadings().get(0).getLemma());

    AnalyzedTokenReadings aToken13 = tagger.lookup("Entweder-oder");
    assertTrue(aToken13.getReadings().get(0).getPOSTag().matches("SUB.*"));
    assertEquals("Entweder-oder", aToken13.getReadings().get(0).getLemma());

    AnalyzedTokenReadings aToken14 = tagger.lookup("Verletzter");
    assertTrue(aToken14.getReadings().get(0).getPOSTag().equals("SUB:NOM:SIN:MAS:ADJ"));
    assertEquals("Verletzter", aToken14.getReadings().get(0).getLemma());
    assertTrue(aToken14.getReadings().get(1).getPOSTag().equals("SUB:GEN:PLU:MAS:ADJ"));

    AnalyzedTokenReadings aToken15 = tagger.lookup("erzkatholisch");
    assertTrue(aToken15.getReadings().get(0).getPOSTag().equals("ADJ:PRD:GRU"));

    AnalyzedTokenReadings aToken16 = tagger.lookup("unerbeten");
    assertTrue(aToken16.getReadings().get(0).getPOSTag().equals("PA2:PRD:GRU:VER"));

    AnalyzedTokenReadings aToken17 = tagger.lookup("under");
    assertTrue(aToken17 == null);
    
    // tag old forms
    AnalyzedTokenReadings aToken18 = tagger.lookup("Zuge");
    assertEquals("Zuge[Zug/SUB:DAT:SIN:MAS]", toSortedString(aToken18));
    AnalyzedTokenReadings aToken19 = tagger.lookup("Tische");
    assertEquals("Tische[Tisch/SUB:AKK:PLU:MAS, Tisch/SUB:DAT:SIN:MAS, Tisch/SUB:GEN:PLU:MAS, Tisch/SUB:NOM:PLU:MAS]", toSortedString(aToken19));

    assertNull(tagger.lookup("vanillig-karamelligen"));

    AnalyzedTokenReadings aToken20 = tagger.lookup("Polizeimitarbeitende");
    assertEquals("Polizeimitarbeitende[Polizeimitarbeitende/SUB:AKK:SIN:FEM:ADJ, Polizeimitarbeitende/SUB:AKK:SIN:NEU:ADJ, " +
      "Polizeimitarbeitende/SUB:NOM:SIN:FEM:ADJ, Polizeimitarbeitende/SUB:NOM:SIN:MAS:ADJ, Polizeimitarbeitende/SUB:NOM:SIN:NEU:ADJ]", toSortedString(aToken20));
    AnalyzedTokenReadings aToken21 = tagger.lookup("Polizeimitarbeitenden");
    assertEquals("Polizeimitarbeitenden[Polizeimitarbeitende/SUB:AKK:PLU:FEM:ADJ, Polizeimitarbeitende/SUB:AKK:PLU:MAS:ADJ, " +
      "Polizeimitarbeitende/SUB:AKK:PLU:NEU:ADJ, Polizeimitarbeitende/SUB:AKK:SIN:MAS:ADJ, Polizeimitarbeitende/SUB:DAT:PLU:FEM:ADJ, " +
      "Polizeimitarbeitende/SUB:DAT:PLU:MAS:ADJ, Polizeimitarbeitende/SUB:DAT:PLU:NEU:ADJ, Polizeimitarbeitende/SUB:DAT:SIN:FEM:ADJ, " +
      "Polizeimitarbeitende/SUB:DAT:SIN:MAS:ADJ, Polizeimitarbeitende/SUB:DAT:SIN:NEU:ADJ, Polizeimitarbeitende/SUB:GEN:PLU:FEM:ADJ, " +
      "Polizeimitarbeitende/SUB:GEN:PLU:MAS:ADJ, Polizeimitarbeitende/SUB:GEN:PLU:NEU:ADJ, Polizeimitarbeitende/SUB:GEN:SIN:FEM:ADJ, " +
      "Polizeimitarbeitende/SUB:GEN:SIN:MAS:ADJ, Polizeimitarbeitende/SUB:GEN:SIN:NEU:ADJ, Polizeimitarbeitende/SUB:NOM:PLU:FEM:ADJ, " +
      "Polizeimitarbeitende/SUB:NOM:PLU:MAS:ADJ, Polizeimitarbeitende/SUB:NOM:PLU:NEU:ADJ]", toSortedString(aToken21));
  }

  // make sure we use the version of the POS data that was extended with post spelling reform data
  @Test
  public void testExtendedTagger() throws IOException {
    assertEquals("Kuß[Kuß/SUB:AKK:SIN:MAS, Kuß/SUB:DAT:SIN:MAS, Kuß/SUB:NOM:SIN:MAS]", toSortedString(tagger.lookup("Kuß")));
    assertEquals("Kuss[Kuss/SUB:AKK:SIN:MAS, Kuss/SUB:DAT:SIN:MAS, Kuss/SUB:NOM:SIN:MAS]", toSortedString(tagger.lookup("Kuss")));

    assertEquals("Haß[Haß/SUB:AKK:SIN:MAS, Haß/SUB:DAT:SIN:MAS, Haß/SUB:NOM:SIN:MAS]", toSortedString(tagger.lookup("Haß")));
    assertEquals("Hass[Hass/SUB:AKK:SIN:MAS, Hass/SUB:DAT:SIN:MAS, Hass/SUB:NOM:SIN:MAS]", toSortedString(tagger.lookup("Hass")));
  }

  @Test
  public void testAfterColon() throws IOException {
    // a colon doesn't start a new sentence in LT, but often it should, so we check the special case for that
    List<AnalyzedTokenReadings> tags = tagger.tag(Arrays.asList("Er", "sagte", ":", "Als", "Erstes", "würde", "ich"));
    assertEquals(7, tags.size());
    assertEquals("Als", tags.get(3).getToken());
    assertEquals(4, tags.get(3).getReadings().size());
  }

  @Test
  public void testTaggerBaseforms() throws IOException {
    List<AnalyzedToken> readings1 = tagger.lookup("übrigbleibst").getReadings();
    assertEquals(1, readings1.size());
    assertEquals("übrigbleiben", readings1.get(0).getLemma());

    List<AnalyzedToken> readings2 = tagger.lookup("Haus").getReadings();
    assertEquals(3, readings2.size());
    assertEquals("Haus", readings2.get(0).getLemma());
    assertEquals("Haus", readings2.get(1).getLemma());
    assertEquals("Haus", readings2.get(2).getLemma());

    List<AnalyzedToken> readings3 = tagger.lookup("Häuser").getReadings();
    assertEquals(3, readings3.size());
    assertEquals("Haus", readings3.get(0).getLemma());
    assertEquals("Haus", readings3.get(1).getLemma());
    assertEquals("Haus", readings3.get(2).getLemma());
  }

  @Test
  public void testTag() throws IOException {
    List<String> upperCaseWord = Arrays.asList("Das");
    List<AnalyzedTokenReadings> readings = tagger.tag(upperCaseWord, false);
    assertEquals("[Das[Das/null*]]", readings.toString());
    List<AnalyzedTokenReadings> readings2 = tagger.tag(upperCaseWord, true);
    assertTrue(readings2.toString().startsWith("[Das[der/ART:"));
  }

  @Test
  public void testTagWithManualDictExtension() throws IOException {
    // words not originally in Morphy but added in LT 1.8 (moved from added.txt to german.dict)
    List<AnalyzedTokenReadings> readings = tagger.tag(Collections.singletonList("Wichtigtuerinnen"));
    assertEquals("[Wichtigtuerinnen[Wichtigtuerin/SUB:AKK:PLU:FEM*," +
            "Wichtigtuerin/SUB:DAT:PLU:FEM*,Wichtigtuerin/SUB:GEN:PLU:FEM*,Wichtigtuerin/SUB:NOM:PLU:FEM*]]", readings.toString());
  }

  @Test
  public void testDictionary() throws IOException {
    Dictionary dictionary = Dictionary.read(
            JLanguageTool.getDataBroker().getFromResourceDirAsUrl("/de/german.dict"));
    DictionaryLookup dl = new DictionaryLookup(dictionary);
    for (WordData wd : dl) {
      if (wd.getTag() == null || wd.getTag().length() == 0) {
        System.err.println("**** Warning: the word " + wd.getWord() + "/" + wd.getStem()
                + " lacks a POS tag in the dictionary.");
      }
    }
  }

  @Test
  public void testIsWeiseException() {
    assertFalse(tagger.isWeiseException("überweise"));
    assertFalse(tagger.isWeiseException("verweise"));
    assertFalse(tagger.isWeiseException("eimerweise"));
    assertFalse(tagger.isWeiseException("meterweise"));
    assertFalse(tagger.isWeiseException("literweise"));
    assertFalse(tagger.isWeiseException("blätterweise"));
    assertFalse(tagger.isWeiseException("erweise"));

    assertTrue(tagger.isWeiseException("lustigerweise"));
    assertTrue(tagger.isWeiseException("idealerweise"));
  }

  @Test
  public void testPrefixVerbsFromSpellingTxt() throws IOException {
    List<AnalyzedTokenReadings> result0 = tagger.tag(Collections.singletonList("herausfallen"));
    assertThat(result0.size(), is(1));
    assertThat(result0.get(0).getReadings().size(), is(5));
    String res0 = result0.toString();
    assertTrue(res0.contains("herausfallen/VER:1:PLU:KJ1:NON:NEB*"));
    assertTrue(res0.contains("herausfallen/VER:1:PLU:PRÄ:NON:NEB*"));
    assertTrue(res0.contains("herausfallen/VER:3:PLU:KJ1:NON:NEB*"));
    assertTrue(res0.contains("herausfallen/VER:3:PLU:PRÄ:NON:NEB*"));
    assertTrue(res0.contains("herausfallen/VER:INF:NON*"));
    assertFalse(res0.contains("ADJ:"));
    assertFalse(res0.contains("PA1:"));
    assertFalse(res0.contains("PA2:"));

    List<AnalyzedTokenReadings> result1 = tagger.tag(Collections.singletonList("herumgeben"));
    assertThat(result1.size(), is(1));
    assertThat(result1.get(0).getReadings().size(), is(5));
    String res1 = result1.toString();
    assertTrue(res1.contains("herumgeben/VER:1:PLU:KJ1:NON:NEB*"));
    assertTrue(res1.contains("herumgeben/VER:1:PLU:PRÄ:NON:NEB*"));
    assertTrue(res1.contains("herumgeben/VER:3:PLU:KJ1:NON:NEB*"));
    assertTrue(res1.contains("herumgeben/VER:3:PLU:PRÄ:NON:NEB*"));
    assertTrue(res1.contains("herumgeben/VER:INF:NON*"));
    assertFalse(res1.contains("ADJ:"));
    assertFalse(res1.contains("PA1:"));
    assertFalse(res1.contains("PA2:"));

    List<AnalyzedTokenReadings> result2 = tagger.tag(Collections.singletonList("herumgab"));
    assertThat(result2.size(), is(1));
    assertThat(result2.get(0).getReadings().size(), is(2));
    String res2 = result2.toString();
    assertTrue(res2.contains("herumgeben/VER:1:SIN:PRT:NON:NEB*"));
    assertTrue(res2.contains("herumgeben/VER:3:SIN:PRT:NON:NEB*"));
    assertFalse(res2.contains("ADJ:"));

    List<AnalyzedTokenReadings> result3 = tagger.tag(Collections.singletonList("zurückgeschickt"));
    assertThat(result3.size(), is(1));
    assertThat(result3.get(0).getReadings().size(), is(2));
    String res3 = result3.toString();
    assertTrue(res3.contains("zurückschicken/VER:PA2:SFT*"));
    assertTrue(res3.contains("PA2:PRD:GRU:VER*"));
    assertFalse(res3.contains("ADJ:"));

    List<AnalyzedTokenReadings> result4 = tagger.tag(Collections.singletonList("abzuschicken"));
    assertThat(result4.size(), is(1));
    assertThat(result4.get(0).getReadings().size(), is(1));
    String res4 = result4.toString();
    assertTrue(res4.contains("abschicken/VER:EIZ:SFT"));
    assertFalse(res4.contains("ADJ:"));

    List<AnalyzedTokenReadings> result4b = tagger.tag(Collections.singletonList("reinzunehmen"));
    assertThat(result4b.size(), is(1));
    assertThat(result4b.get(0).getReadings().size(), is(1));
    String res4b = result4b.toString();
    assertTrue(res4b.contains("reinnehmen/VER:EIZ:NON"));
    assertFalse(res4b.contains("ADJ:"));

    List<AnalyzedTokenReadings> result5 = tagger.tag(Collections.singletonList("Mitmanagen"));
    assertThat(result5.size(), is(1));
    assertThat(result5.get(0).getReadings().size(), is(3));
    String res5 = result5.toString();
    assertTrue(res5.contains("Mitmanagen/SUB:NOM:SIN:NEU:INF"));
    assertTrue(res5.contains("Mitmanagen/SUB:AKK:SIN:NEU:INF"));
    assertTrue(res5.contains("Mitmanagen/SUB:DAT:SIN:NEU:INF"));
    assertFalse(res5.contains("ADJ:"));

    List<AnalyzedTokenReadings> result6 = tagger.tag(Collections.singletonList("Mitmanagens"));
    assertThat(result6.size(), is(1));
    assertThat(result6.get(0).getReadings().size(), is(1));
    String res6 = result6.toString();
    assertTrue(res6.contains("Mitmanagen/SUB:GEN:SIN:NEU:INF"));
    assertFalse(res6.contains("ADJ:"));

    List<AnalyzedTokenReadings> result7 = tagger.tag(Collections.singletonList("Wegstrecken"));
    assertThat(result7.size(), is(1));
    assertThat(result7.get(0).getReadings().size(), is(7));
    String res7 = result7.toString();
    assertFalse(res7.contains("Wegstrecken/SUB:GEN:SIN:NEU:INF"));
  }

  @Test
  public void testPrefixVerbsSeparable() throws IOException {
    List<AnalyzedTokenReadings> result1 = tagger.tag(Collections.singletonList("nachguckst"));
    assertThat(result1.size(), is(1));
    assertThat(result1.get(0).getReadings().size(), is(1));
    String res1 = result1.toString();
    assertTrue(res1.contains("nachgucken/VER:2:SIN:PRÄ:SFT:NEB"));
    assertFalse(res1.contains("ADJ:"));
    assertFalse(res1.contains("PA1:"));
    assertFalse(res1.contains("PA2:"));

    List<AnalyzedTokenReadings> result2 = tagger.tag(Collections.singletonList("nachgucke"));
    assertThat(result2.size(), is(1));
    assertThat(result2.get(0).getReadings().size(), is(4));
    String res2 = result2.toString();
    assertTrue(res2.contains("nachgucken/VER:1:SIN:PRÄ:SFT:NEB*"));
    assertTrue(res2.contains("nachgucken/VER:1:SIN:KJ1:SFT:NEB*"));
    assertTrue(res2.contains("nachgucken/VER:3:SIN:KJ1:SFT:NEB*"));
    assertFalse(res2.contains("nachgucken/VER:1:SIN:PRÄ:SFT*"));
    assertFalse(res2.contains("nachgucken/VER:1:SIN:KJ1:SFT*"));
    assertFalse(res2.contains("nachgucken/VER:3:SIN:KJ1:SFT*"));
    assertFalse(res2.contains("nachgucken/VER:IMP:SIN:SFT*"));
    assertFalse(res2.contains("nachgucken/VER:IMP:SIN:SFT:NEB*"));
    assertFalse(res2.contains("ADJ:"));
    assertFalse(res2.contains("PA1:"));
    assertFalse(res2.contains("PA2:"));

    List<AnalyzedTokenReadings> result3 = tagger.tag(Collections.singletonList("nachzugucken"));
    assertThat(result3.size(), is(1));
    assertThat(result3.get(0).getReadings().size(), is(1));
    String res3 = result3.toString();
    assertTrue(res3.contains("nachgucken/VER:EIZ:SFT*"));
    assertFalse(res3.contains("nachgucken/VER:INF:SFT*"));
    assertFalse(res3.contains("nachgucken/VER:INF:SFT*"));
    assertFalse(res3.contains("nachzugucken/VER:1:PLU:PRÄ:SFT*"));
    assertFalse(res3.contains("nachzugucken/VER:1:PLU:KJ1:SFT*"));
    assertFalse(res3.contains("nachzugucken/VER:3:PLU:PRÄ:SFT*"));
    assertFalse(res3.contains("nachzugucken/VER:3:PLU:KJ1:SFT*"));

    List<AnalyzedTokenReadings> result4 = tagger.tag(Collections.singletonList("nachguckend"));
    assertThat(result4.size(), is(1));
    assertThat(result4.get(0).getReadings().size(), is(2));
    String res4 = result4.toString();
    assertTrue(res4.contains("nachgucken/VER:PA1:SFT*"));
    assertTrue(res4.contains("nachguckend/PA1:PRD:GRU:VER*"));
    assertFalse(res4.contains("nachgucken/VER:PA1:SFT:NEB*"));

    List<AnalyzedTokenReadings> result5 = tagger.tag(Collections.singletonList("nachgeguckt"));
    assertThat(result5.size(), is(1));
    assertThat(result5.get(0).getReadings().size(), is(2));
    String res5 = result5.toString();
    assertTrue(res5.contains("nachgucken/VER:PA2:SFT*"));
    assertTrue(res5.contains("nachgeguckt/PA2:PRD:GRU:VER*"));
    assertFalse(res5.contains("nachgucken/VER:PA2:SFT:NEB*"));

    List<AnalyzedTokenReadings> result6 = tagger.tag(Collections.singletonList("Nachgucken"));
    assertThat(result6.size(), is(1));
    assertThat(result6.get(0).getReadings().size(), is(3));
    String res6 = result6.toString();
    assertTrue(res6.contains("Nachgucken/SUB:NOM:SIN:NEU:INF*"));
    assertTrue(res6.contains("Nachgucken/SUB:DAT:SIN:NEU:INF*"));
    assertTrue(res6.contains("Nachgucken/SUB:AKK:SIN:NEU:INF*"));

    List<AnalyzedTokenReadings> result7 = tagger.tag(Collections.singletonList("Nachguckens"));
    assertThat(result7.size(), is(1));
    assertThat(result7.get(0).getReadings().size(), is(1));
    String res7 = result7.toString();
    assertTrue(res7.contains("Nachgucken/SUB:GEN:SIN:NEU:INF*"));

    List<AnalyzedTokenReadings> result8 = tagger.tag(Collections.singletonList("Nachzudenken"));
    assertThat(result8.size(), is(1));
    assertThat(result8.get(0).getReadings().size(), is(1));
    String res8 = result8.toString();
    assertTrue(res8.contains("nachdenken/VER:EIZ:NON*"));
    assertFalse(res8.contains("Nachzudenken/SUB:NOM:SIN:NEU:INF*"));
    assertFalse(res8.contains("Nachzudenken/SUB:DAT:SIN:NEU:INF*"));
    assertFalse(res8.contains("Nachzudenken/SUB:AKK:SIN:NEU:INF*"));

    List<AnalyzedTokenReadings> result9 = tagger.tag(Collections.singletonList("entlangblicken"));
    assertThat(result9.size(), is(1));
    assertThat(result9.get(0).getReadings().size(), is(5));
    String res9 = result9.toString();
    assertTrue(res9.contains("entlangblicken/VER:INF:SFT*"));
    assertTrue(res9.contains("NEB*"));

    List<AnalyzedTokenReadings> result10 = tagger.tag(Collections.singletonList("wiederaufbauen"));
    assertThat(result10.size(), is(1));
    assertThat(result10.get(0).getReadings().size(), is(5));
    String res10 = result10.toString();
    assertTrue(res10.contains("NEB"));
    assertFalse(res10.contains("NEB:NEB"));
  }

  @Test
  public void testPrefixVerbsNotMod() throws IOException {
    List<AnalyzedTokenReadings> result1 = tagger.tag(Collections.singletonList("rauslassen"));
    assertThat(result1.size(), is(1));
    assertThat(result1.get(0).getReadings().size(), is(5));
    String res1 = result1.toString();
    assertTrue(res1.contains("rauslassen/VER:1:PLU:PRÄ:NON:NEB*"));
    assertTrue(res1.contains("rauslassen/VER:1:PLU:KJ1:NON:NEB*"));
    assertTrue(res1.contains("rauslassen/VER:3:PLU:PRÄ:NON:NEB*"));
    assertTrue(res1.contains("rauslassen/VER:3:PLU:KJ1:NON:NEB*"));
    assertTrue(res1.contains("rauslassen/VER:INF:NON*"));
    assertFalse(res1.contains("rauslassen/VER:1:PLU:PRÄ:NON*"));
    assertFalse(res1.contains("rauslassen/VER:1:PLU:KJ1:NON*"));
    assertFalse(res1.contains("rauslassen/VER:3:PLU:PRÄ:NON*"));
    assertFalse(res1.contains("rauslassen/VER:3:PLU:KJ1:NON*"));
    assertFalse(res1.contains("rauslassen/VER:MOD:1:PLU:PRÄ*"));
    assertFalse(res1.contains("rauslassen/VER:MOD:1:PLU:KJ1*"));
    assertFalse(res1.contains("rauslassen/VER:MOD:3:PLU:PRÄ*"));
    assertFalse(res1.contains("rauslassen/VER:MOD:3:PLU:KJ1*"));
    assertFalse(res1.contains("rauslassen/VER:MOD:INF*"));

    List<AnalyzedTokenReadings> result2 = tagger.tag(Collections.singletonList("rauslass"));
    assertThat(result2.size(), is(1));
    assertThat(result2.get(0).getReadings().size(), is(2));
    String res2 = result2.toString();
    assertTrue(res2.contains("rauslassen/VER:1:SIN:PRÄ:NON:NEB*"));
    assertFalse(res2.contains("rauslassen/VER:IMP:SIN:NON*"));
    assertFalse(res2.contains("rauslassen/VER:MOD:IMP:SIN"));
    assertFalse(res2.contains("rauslassen/VER:IMP:SIN:NON:NEB*"));

    List<AnalyzedTokenReadings> result3 = tagger.tag(Collections.singletonList("rauszulassen"));
    assertThat(result3.size(), is(1));
    assertThat(result3.get(0).getReadings().size(), is(1));
    String res3 = result3.toString();
    assertTrue(res3.contains("rauslassen/VER:EIZ:NON*"));

    List<AnalyzedTokenReadings> result4 = tagger.tag(Collections.singletonList("rausgelassen"));
    assertThat(result4.size(), is(1));
    assertThat(result4.get(0).getReadings().size(), is(2));
    String res4 = result4.toString();
    assertTrue(res4.contains("rauslassen/VER:PA2:NON*"));
    assertTrue(res4.contains("rausgelassen/PA2:PRD:GRU:VER*"));
    assertFalse(res4.contains("rauslassen/VER:MOD:PA2"));
  }

  @Test
  public void testPrefixVerbsNonSeparable() throws IOException {
    List<AnalyzedTokenReadings> result1 = tagger.tag(Collections.singletonList("vergären"));
    assertThat(result1.size(), is(1));
    assertThat(result1.get(0).getReadings().size(), is(15));
    String res1 = result1.toString();
    assertTrue(res1.contains("vergären/VER:1:PLU:PRÄ:NON*"));
    assertTrue(res1.contains("vergären/VER:1:PLU:KJ1:NON*"));
    assertTrue(res1.contains("vergären/VER:3:PLU:PRÄ:NON*"));
    assertTrue(res1.contains("vergären/VER:3:PLU:KJ1:NON*"));
    assertTrue(res1.contains("vergären/VER:INF:NON*"));
    assertTrue(res1.contains("vergären/VER:1:PLU:PRÄ:SFT*"));
    assertTrue(res1.contains("vergären/VER:1:PLU:KJ1:SFT*"));
    assertTrue(res1.contains("vergären/VER:3:PLU:PRÄ:SFT*"));
    assertTrue(res1.contains("vergären/VER:3:PLU:KJ1:SFT*"));
    assertTrue(res1.contains("vergären/VER:INF:SFT*"));

    List<AnalyzedTokenReadings> result2 = tagger.tag(Collections.singletonList("Vergären"));
    assertThat(result2.size(), is(1));
    assertThat(result2.get(0).getReadings().size(), is(24));
    String res2 = result2.toString();
    assertTrue(res2.contains("Vergären/SUB:NOM:SIN:NEU:INF"));
    assertTrue(res2.contains("Vergären/SUB:DAT:SIN:NEU:INF"));
    assertTrue(res2.contains("Vergären/SUB:AKK:SIN:NEU:INF"));
    assertTrue(res2.contains("vergären/VER:1:PLU:PRÄ:NON*"));
    assertTrue(res2.contains("vergären/VER:1:PLU:KJ1:NON*"));
    assertTrue(res2.contains("vergären/VER:3:PLU:PRÄ:NON*"));
    assertTrue(res2.contains("vergären/VER:3:PLU:KJ1:NON*"));
    assertTrue(res2.contains("vergären/VER:INF:SFT*"));
    assertTrue(res2.contains("vergären/VER:1:PLU:PRÄ:SFT*"));
    assertTrue(res2.contains("vergären/VER:1:PLU:KJ1:SFT*"));
    assertTrue(res2.contains("vergären/VER:3:PLU:PRÄ:SFT*"));
    assertTrue(res2.contains("vergären/VER:3:PLU:KJ1:SFT*"));
    assertTrue(res2.contains("vergären/VER:INF:SFT*"));

    List<AnalyzedTokenReadings> result3 = tagger.tag(Collections.singletonList("unbeglückt"));
    assertThat(result3.size(), is(1));
    assertThat(result3.get(0).getReadings().size(), is(2));
    String res3 = result3.toString();
    assertTrue(res3.contains("PA2:PRD:GRU:VER"));
    assertFalse(res3.contains("VER:"));

    List<AnalyzedTokenReadings> result4 = tagger.tag(Collections.singletonList("erstritten"));
    assertThat(result4.size(), is(1));
    assertThat(result4.get(0).getReadings().size(), is(6));
    String res4 = result4.toString();
    assertTrue(res4.contains("erstreiten/VER:1:PLU:PRT:NON"));
    assertTrue(res4.contains("erstreiten/VER:PA2:NON"));
    assertTrue(res4.contains("erstritten/PA2:PRD:GRU:VER"));

    List<AnalyzedTokenReadings> result5 = tagger.tag(Collections.singletonList("bemessen"));
    assertThat(result5.size(), is(1));
    assertThat(result5.get(0).getReadings().size(), is(7));
    String res5 = result5.toString();
    assertTrue(res5.contains("bemessen/VER:INF:NON"));
    assertTrue(res5.contains("bemessen/VER:PA2:NON"));
    assertTrue(res5.contains("bemessen/PA2:PRD:GRU:VER"));

    List<AnalyzedTokenReadings> result6 = tagger.tag(Collections.singletonList("bemisst"));
    assertThat(result6.size(), is(1));
    assertThat(result6.get(0).getReadings().size(), is(8));
    String res6 = result6.toString();
    assertFalse(res6.contains("bemessen/PA2:PRD:GRU:VER"));

    List<AnalyzedTokenReadings> result7 = tagger.tag(Collections.singletonList("hinterlass"));
    assertThat(result7.size(), is(1));
    assertThat(result7.get(0).getReadings().size(), is(3));
    String res7 = result7.toString();
    assertTrue(res7.contains("hinterlassen/VER:IMP:SIN:NON"));
    assertTrue(res7.contains("hinterlassen/VER:1:SIN:PRÄ:NON"));
    assertFalse(res7.contains("NEB"));
  }

  @Test
  public void testNoVerb() throws IOException {
    List<AnalyzedTokenReadings> result1 = tagger.tag(Collections.singletonList("geschichte"));
    assertThat(result1.size(), is(1));
    assertThat(result1.get(0).getReadings().size(), is(1));
    String res1 = result1.toString();
    assertTrue(res1.contains(""));
    assertFalse(res1.contains("VER"));

    List<AnalyzedTokenReadings> result2 = tagger.tag(Collections.singletonList("bereich"));
    assertThat(result2.size(), is(1));
    assertThat(result2.get(0).getReadings().size(), is(1));
    String res2 = result2.toString();
    assertTrue(res2.contains(""));
    assertFalse(res2.contains("VER"));

    List<AnalyzedTokenReadings> result3 = tagger.tag(Collections.singletonList("beispiel"));
    assertThat(result3.size(), is(1));
    assertThat(result3.get(0).getReadings().size(), is(1));
    String res3 = result3.toString();
    assertTrue(res3.contains(""));
    assertFalse(res3.contains("VER"));

    List<AnalyzedTokenReadings> result4 = tagger.tag(Collections.singletonList("keksdose"));
    assertThat(result4.size(), is(1));
    assertThat(result4.get(0).getReadings().size(), is(1));
    String res4 = result4.toString();
    assertTrue(res4.contains(""));
    assertFalse(res4.contains("VER"));

    List<AnalyzedTokenReadings> result5 = tagger.tag(Collections.singletonList("aktienarten"));
    assertThat(result5.size(), is(1));
    assertThat(result5.get(0).getReadings().size(), is(1));
    String res5 = result5.toString();
    assertTrue(res5.contains(""));
    assertFalse(res5.contains("VER"));

    List<AnalyzedTokenReadings> result6 = tagger.tag(Collections.singletonList("schwarzgrau"));
    assertThat(result6.size(), is(1));
    assertThat(result6.get(0).getReadings().size(), is(1));
    String res6 = result6.toString();
    assertTrue(res6.contains(""));
    assertFalse(res6.contains("VER"));

    List<AnalyzedTokenReadings> result7 = tagger.tag(Collections.singletonList("unmenge"));
    assertThat(result7.size(), is(1));
    assertThat(result7.get(0).getReadings().size(), is(1));
    String res7 = result7.toString();
    assertTrue(res7.contains(""));
    assertFalse(res7.contains("VER"));

    List<AnalyzedTokenReadings> result8 = tagger.tag(Collections.singletonList("entlang"));
    assertThat(result8.size(), is(1));
    assertThat(result8.get(0).getReadings().size(), is(4));
    String res8 = result8.toString();
    assertTrue(res8.contains("PRP"));
    assertTrue(res8.contains("ZUS"));
    assertFalse(res8.contains("VER"));
  }

  @Test
  public void testVerbAndPa2() throws IOException {
    List<AnalyzedTokenReadings> result1 = tagger.tag(Collections.singletonList("erstickt"));
    assertThat(result1.size(), is(1));
    assertThat(result1.get(0).getReadings().size(), is(5));
    String res1 = result1.toString();
    assertTrue(res1.contains("ersticken/VER:2:PLU:PRÄ:SFT"));
    assertTrue(res1.contains("ersticken/VER:3:SIN:PRÄ:SFT"));
    assertTrue(res1.contains("ersticken/VER:IMP:PLU:SFT"));
    assertTrue(res1.contains("ersticken/VER:PA2:SFT"));
    assertTrue(res1.contains("erstickt/PA2:PRD:GRU:VER"));

    List<AnalyzedTokenReadings> result2 = tagger.tag(Collections.singletonList("erstickte"));
    assertThat(result2.size(), is(1));
    assertThat(result2.get(0).getReadings().size(), is(19));
    String res2 = result2.toString();
    assertTrue(res2.contains("erstickt/PA2"));
    assertTrue(res2.contains("ersticken/VER:1:SIN:KJ2:SFT"));
    assertTrue(res2.contains("ersticken/VER:1:SIN:PRT:SFT"));
    assertTrue(res2.contains("ersticken/VER:3:SIN:KJ2:SFT"));
    assertTrue(res2.contains("ersticken/VER:3:SIN:PRT:SFT"));
    assertFalse(res2.contains("NEB"));

    List<AnalyzedTokenReadings> result3 = tagger.tag(Collections.singletonList("erstickend"));
    assertThat(result3.size(), is(1));
    assertThat(result3.get(0).getReadings().size(), is(2));
    String res3 = result3.toString();
    assertTrue(res3.contains("ersticken/VER:PA1:SFT"));
    assertTrue(res3.contains("erstickend/PA1:PRD:GRU:VER"));

    List<AnalyzedTokenReadings> result4 = tagger.tag(Collections.singletonList("erstickender"));
    assertThat(result4.size(), is(1));
    assertThat(result4.get(0).getReadings().size(), is(7));
    String res4 = result4.toString();
    assertTrue(res4.contains("erstickend/PA1:DAT:SIN:FEM:GRU:SOL:VER"));
  }

  /**
   * Returns a string representation like {@code toString()}, but sorts
   * the elements alphabetically.
   */
  public static String toSortedString(AnalyzedTokenReadings tokenReadings) {
    StringBuilder sb = new StringBuilder(tokenReadings.getToken());
    Set<String> elements = new TreeSet<>();
    sb.append('[');
    for (AnalyzedToken reading : tokenReadings) {
      elements.add(reading.toString());
    }
    sb.append(String.join(", ", elements));
    sb.append(']');
    return sb.toString();
  }
}
