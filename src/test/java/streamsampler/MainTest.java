package streamsampler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static streamsampler.Main.Sampler.*;
import static streamsampler.Main.getRandomInputStream;

class MainTest {

    private static final Random rnd = new Random(12345);

    @Nested
    class sampler {

        @Nested
        class getRepresentatives {
            @Test
            void forOneCharacter() {

                Map<Byte, Long> inputCounters = new HashMap<>();
                inputCounters.put((byte) 'f', 1L);

                List<Byte> sample = getRepresentatives(inputCounters, Arrays.asList(0L, 0L, 0L));

                Assertions.assertEquals(Arrays.asList((byte) 'f', (byte) 'f', (byte) 'f'), sample);
            }

            @Test
            void forMultipleCharacters() {

                Map<Byte, Long> inputCounters = new HashMap<>();
                inputCounters.put((byte) 'a', 1L);
                inputCounters.put((byte) 'b', 2L);
                inputCounters.put((byte) 'c', 3L);
                inputCounters.put((byte) 'd', 4L);
                inputCounters.put((byte) 'e', 5L);

                List<Byte> sample = getRepresentatives(inputCounters, Arrays.asList(4L, 8L, 11L, 13L, 14L));

                Assertions.assertEquals(Arrays.asList((byte) 'e', (byte) 'd', (byte) 'c', (byte) 'b', (byte) 'a'), sample);

            }
        }

        @Nested
        class sampleStream {
            @Test
            void singleCharacter() {
                InputStream in = new ByteArrayInputStream("AAA".getBytes());
                final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(outStream);

                sampleStream(in, out, 5, new Random());

                Assertions.assertEquals("AAAAA", outStream.toString());
            }

            @RepeatedTest(100)
            void manyCharacters() {
                final String inputStream = "THEQUICKBROWNFOXJUMPEDOVERTHELAZYDOG";
                InputStream in = new ByteArrayInputStream(inputStream.getBytes());
                final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(outStream);

                sampleStream(in, out, 5, new Random());


                for (Byte b : outStream.toByteArray()) {
                    final String ch = String.valueOf((char) b.byteValue());
                    Assertions.assertTrue(inputStream.contains(ch), String.format("Unexpected character %s (byte %s)", ch, b));
                }

            }

            @Test
            void nonAscii() {
                final String inputStream = "Ąćźżółöäß";
                InputStream in = new ByteArrayInputStream(inputStream.getBytes());
                final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(outStream);

                sampleStream(in, out, 5, rnd);

                Assertions.assertEquals("ﾂￃﾤￄￅ", outStream.toString());
            }

            @Test
            void emptyInput() {
                final String inputStream = "";
                InputStream in = new ByteArrayInputStream(inputStream.getBytes());
                final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(outStream);

                sampleStream(in, out, 5, new Random());

                Assertions.assertEquals("", outStream.toString());
            }
        }

        @Nested
        class readInputStream {

            @Test
            void okStream() {
                InputStream s = new ByteArrayInputStream("ABBCCCDDDDEEEEE".getBytes());
                Map<Byte, Long> counters = readInputStream(s);

                Map<Byte, Long> expected = new HashMap<>();
                expected.put((byte)'A', 1L);
                expected.put((byte)'B', 2L);
                expected.put((byte)'C', 3L);
                expected.put((byte)'D', 4L);
                expected.put((byte)'E', 5L);

                Assertions.assertEquals(expected, counters);
            }

            @Test
            void emptyStream() {
                InputStream s = new ByteArrayInputStream("".getBytes());
                Map<Byte, Long> counters = readInputStream(s);

                Assertions.assertEquals(Collections.emptyMap(), counters);
            }

            @Test
            void failingStream() {
                InputStream s = new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("Gotcha!");
                    }
                };

                Assertions.assertThrows(RuntimeException.class, () -> readInputStream(s));

            }
        }
    }

    @Nested
    class randomInputStream {

        @Test
        void length20() {
            final InputStream randomStream = getRandomInputStream(20L, rnd);
            String x = new BufferedReader(new InputStreamReader(randomStream)).lines().collect(Collectors.joining());
            Assertions.assertEquals("urjmhehtnxfgyywrlhwp", x);
        }

        @Test
        void empty() {
            final InputStream randomStream = getRandomInputStream(0L, rnd);
            String x = new BufferedReader(new InputStreamReader(randomStream)).lines().collect(Collectors.joining());
            Assertions.assertEquals("", x);
        }
    }

}
