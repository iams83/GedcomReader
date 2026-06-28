package iamd.gedcom.datamodel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import iamd.gedcom.datamodel.DateTime.Month;
import iamd.gedcom.datamodel.DateTime.Uncertainty;
import iamd.gedcom.format.GedComParseException;

class DateParserTest
{
    @Nested
    class StandardMonths
    {
        @Test
        void fullDate() throws GedComParseException
        {
            var p = new DateTime.DateParser("1 JAN 1900", false);
            assertAll(
                () -> assertEquals(1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(1900, p.year),
                () -> assertEquals(-1, p.rangeMaxYear),
                () -> assertNull(p.uncertainty)
            );
        }

        @Test
        void monthAndYear() throws GedComParseException
        {
            var p = new DateTime.DateParser("JAN 1900", false);
            assertAll(
                () -> assertEquals(-1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(1900, p.year),
                () -> assertNull(p.uncertainty)
            );
        }

        @Test
        void yearOnly() throws GedComParseException
        {
            var p = new DateTime.DateParser("1900", false);
            assertAll(
                () -> assertEquals(-1, p.day),
                () -> assertNull(p.month),
                () -> assertEquals(1900, p.year),
                () -> assertNull(p.uncertainty)
            );
        }

        @Test
        void allMonths() throws GedComParseException
        {
            for (var m : Month.values())
            {
                var p = new DateTime.DateParser("1 " + m.name() + " 2000", false);
                assertEquals(m, p.month);
            }
        }

        @Test
        void december() throws GedComParseException
        {
            var p = new DateTime.DateParser("25 DEC 2023", false);
            assertAll(
                () -> assertEquals(25, p.day),
                () -> assertEquals(Month.DEC, p.month),
                () -> assertEquals(2023, p.year)
            );
        }
    }

    @Nested
    class UncertaintyTests
    {
        @Test
        void about() throws GedComParseException
        {
            var p = new DateTime.DateParser("ABT 1900", false);
            assertAll(
                () -> assertEquals(Uncertainty.ABT, p.uncertainty),
                () -> assertEquals(1900, p.year)
            );
        }

        @Test
        void before() throws GedComParseException
        {
            var p = new DateTime.DateParser("BEF 1900", false);
            assertEquals(Uncertainty.BEF, p.uncertainty);
        }

        @Test
        void after() throws GedComParseException
        {
            var p = new DateTime.DateParser("AFT 1900", false);
            assertEquals(Uncertainty.AFT, p.uncertainty);
        }

        @Test
        void certain() throws GedComParseException
        {
            var p = new DateTime.DateParser("CER 1900", false);
            assertEquals(Uncertainty.CER, p.uncertainty);
        }

        @Test
        void trailingQuestionMark() throws GedComParseException
        {
            var p = new DateTime.DateParser("1900?", false);
            assertAll(
                () -> assertEquals(Uncertainty.ABT, p.uncertainty),
                () -> assertEquals(1900, p.year)
            );
        }

        @Test
        void trailingQuestionMarkWithFullDate() throws GedComParseException
        {
            var p = new DateTime.DateParser("1 JAN 1900?", false);
            assertAll(
                () -> assertEquals(Uncertainty.ABT, p.uncertainty),
                () -> assertEquals(1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(1900, p.year)
            );
        }

        @Test
        void uncertaintyWithFullDate() throws GedComParseException
        {
            var p = new DateTime.DateParser("ABT 1 JAN 1900", false);
            assertAll(
                () -> assertEquals(Uncertainty.ABT, p.uncertainty),
                () -> assertEquals(1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(1900, p.year)
            );
        }
    }

    @Nested
    class Ranges
    {
        @Test
        void betYearRange() throws GedComParseException
        {
            var p = new DateTime.DateParser("BET 1900 AND 1910", false);
            assertAll(
                () -> assertNull(p.uncertainty),
                () -> assertEquals(1900, p.year),
                () -> assertEquals(1910, p.rangeMaxYear)
            );
        }

        @Test
        void yearSlashRange() throws GedComParseException
        {
            var p = new DateTime.DateParser("1900/1910", false);
            assertAll(
                () -> assertEquals(1900, p.year),
                () -> assertEquals(1910, p.rangeMaxYear)
            );
        }

        @Test
        void monthYearSlashRange() throws GedComParseException
        {
            var p = new DateTime.DateParser("JAN 1900/1910", false);
            assertAll(
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(1900, p.year),
                () -> assertEquals(1910, p.rangeMaxYear)
            );
        }

        @Test
        void shortRangeYearNormalized() throws GedComParseException
        {
            var p = new DateTime.DateParser("1900/25", false);
            assertAll(
                () -> assertEquals(1900, p.year),
                () -> assertEquals(1925, p.rangeMaxYear)
            );
        }
    }

    @Nested
    class HumanMonths
    {
        @Test
        void usesLocaleHumanName() throws GedComParseException
        {
            Month jan = Month.JAN;
            String humanName = jan.humanName();
            var p = new DateTime.DateParser("1 " + humanName + " 2000", true);
            assertAll(
                () -> assertEquals(1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(2000, p.year)
            );
        }

        @Test
        void allHumanMonths() throws GedComParseException
        {
            for (var m : Month.values())
            {
                String human = m.humanName();
                var p = new DateTime.DateParser("1 " + human + " 2000", true);
                assertEquals(m, p.month);
            }
        }

        @Test
        void humanNameWithUpperCase() throws GedComParseException
        {
            Month jan = Month.JAN;
            String humanName = jan.humanName().toUpperCase();
            var p = new DateTime.DateParser("1 " + humanName + " 2000", true);
            assertEquals(Month.JAN, p.month);
        }

        @Test
        void humanMonthAndYearOnly() throws GedComParseException
        {
            Month mar = Month.MAR;
            var p = new DateTime.DateParser(mar.humanName() + " 1999", true);
            assertAll(
                () -> assertEquals(-1, p.day),
                () -> assertEquals(Month.MAR, p.month),
                () -> assertEquals(1999, p.year)
            );
        }

        @Test
        void spanishLocale() throws GedComParseException
        {
            Locale original = Locale.getDefault();
            Locale.setDefault(Locale.forLanguageTag("es"));
            try
            {
                String spanishJan = Month.JAN.humanName();
                var p = new DateTime.DateParser("1 " + spanishJan + " 2000", true);
                assertAll(
                    () -> assertEquals(1, p.day),
                    () -> assertEquals(Month.JAN, p.month),
                    () -> assertEquals(2000, p.year)
                );
            }
            finally
            {
                Locale.setDefault(original);
            }
        }

        @Test
        void englishNameInAnyLocale() throws GedComParseException
        {
            var p = new DateTime.DateParser("1 Jan 2000", true);
            assertAll(
                () -> assertEquals(1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(2000, p.year)
            );
        }

        @Test
        void allEnglishHumanNames() throws GedComParseException
        {
            String[] enNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            Month[] months = Month.values();
            for (int i = 0; i < 12; i++)
            {
                var p = new DateTime.DateParser("1 " + enNames[i] + " 2000", true);
                assertEquals(months[i], p.month);
            }
        }

        @Test
        void spanishNameInAnyLocale() throws GedComParseException
        {
            var p = new DateTime.DateParser("1 Ene 2000", true);
            assertAll(
                () -> assertEquals(1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(2000, p.year)
            );
        }

        @Test
        void allSpanishHumanNames() throws GedComParseException
        {
            String[] esNames = {"Ene", "Feb", "Mar", "Abr", "May", "Jun",
                                "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            Month[] months = Month.values();
            for (int i = 0; i < 12; i++)
            {
                var p = new DateTime.DateParser("1 " + esNames[i] + " 2000", true);
                assertEquals(months[i], p.month);
            }
        }

        @Test
        void englishAndSpanishMixedCase() throws GedComParseException
        {
            assertAll(
                () -> assertEquals(Month.JAN, new DateTime.DateParser("1 JAN 2000", true).month),
                () -> assertEquals(Month.JAN, new DateTime.DateParser("1 jan 2000", true).month),
                () -> assertEquals(Month.JAN, new DateTime.DateParser("1 ENE 2000", true).month),
                () -> assertEquals(Month.JAN, new DateTime.DateParser("1 ene 2000", true).month)
            );
        }

    }

    @Nested
    class Whitespace
    {
        @Test
        void extraWhitespace() throws GedComParseException
        {
            var p = new DateTime.DateParser("  1   JAN   1900  ", false);
            assertAll(
                () -> assertEquals(1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(1900, p.year)
            );
        }

        @Test
        void tabsAndNewlines() throws GedComParseException
        {
            var p = new DateTime.DateParser("1\tJAN\n1900", false);
            assertAll(
                () -> assertEquals(1, p.day),
                () -> assertEquals(Month.JAN, p.month),
                () -> assertEquals(1900, p.year)
            );
        }
    }

    @Nested
    class ErrorCases
    {
        @Test
        void nullInput()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser(null, false));
        }

        @Test
        void tooManyTokens()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("1 2 JAN 1900", false));
        }

        @Test
        void invalidMonth()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("1 XXX 1900", false));
        }

        @Test
        void yearTooLarge()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("1 JAN 2101", false));
        }

        @Test
        void dayTooLarge()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("32 JAN 1900", false));
        }

        @Test
        void malformedBetNoAnd()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("BET 1900 1910", false));
        }

        @Test
        void emptyString()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("", false));
        }

        @Test
        void nonNumericYear()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("JAN ABC", false));
        }

        @Test
        void nonNumericDay()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("XX JAN 1900", false));
        }

        @Test
        void uncertaintyWithoutSpace()
        {
            assertThrows(GedComParseException.class,
                () -> new DateTime.DateParser("ABT1900", false));
        }
    }

    @Nested
    class EdgeValues
    {
        @Test
        void firstDayOfMonth() throws GedComParseException
        {
            var p = new DateTime.DateParser("1 JAN 1900", false);
            assertEquals(1, p.day);
        }

        @Test
        void thirtyFirstDay() throws GedComParseException
        {
            var p = new DateTime.DateParser("31 JAN 1900", false);
            assertEquals(31, p.day);
        }

        @Test
        void yearZero() throws GedComParseException
        {
            var p = new DateTime.DateParser("1 JAN 0", false);
            assertEquals(0, p.year);
        }

        @Test
        void year2100() throws GedComParseException
        {
            var p = new DateTime.DateParser("1 JAN 2100", false);
            assertEquals(2100, p.year);
        }

        @Test
        void dayZeroIsAllowed() throws GedComParseException
        {
            var p = new DateTime.DateParser("0 JAN 1900", false);
            assertEquals(0, p.day);
        }

        @Test
        void fourDigitYear() throws GedComParseException
        {
            var p = new DateTime.DateParser("1 JAN 1066", false);
            assertEquals(1066, p.year);
        }
    }
}
