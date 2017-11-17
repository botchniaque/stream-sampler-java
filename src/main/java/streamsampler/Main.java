package streamsampler;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    private static final String ALPHABET = "abcdefghijklmnoprstuwxyz";

    public static void main(String[] args) {

        Options opts = new Options();
        opts.addOption(Option.builder("n")
                .required()
                .longOpt("size")
                .hasArg()
                .type(Number.class)
                .desc("Sample size")
                .argName("SIZE")
                .build());
        opts.addOption(Option.builder("s")
                .longOpt("seed")
                .hasArg()
                .type(Number.class)
                .desc("Seed for random number generator")
                .argName("SEED")
                .build());
        opts.addOption(Option.builder("g")
                .hasArg()
                .type(Number.class)
                .desc(String.format("Size of random input to generate out of '%s'", ALPHABET))
                .longOpt("generate")
                .argName("INPUT_SIZE")
                .build());
        opts.addOption(Option.builder()
                .desc("Show this message")
                .longOpt("help")
                .build());

        try {
            CommandLine cli = new DefaultParser().parse(opts, args);

            if (cli.hasOption("help")) {
                printHelp(opts);
                System.exit(0);
            }

            final Integer sampleSize = ((Long) cli.getParsedOptionValue("size")).intValue();

            final int seed = cli.hasOption("seed")
                    ? ((Long) cli.getParsedOptionValue("seed")).intValue()
                    : new Random().nextInt();

            final Random rnd = new Random(seed);

            final InputStream in = cli.hasOption("generate")
                    ? getRandomInputStream(((Long) cli.getParsedOptionValue("generate")), rnd)
                    : System.in;

            final PrintStream out = System.out;

            Sampler.sampleStream(in, out, sampleSize, rnd);
        } catch (ParseException e) {
            printHelp(opts);
            System.exit(1);
        }
    }

    private static void printHelp(Options opts) {
        new HelpFormatter().printHelp(
                "cat file.txt | ./stream-sampler -n SIZE",
                "Creates a random representative sample of length SIZE out of the input.\n" +
                        "Input is either STDIN, or randomly generated within application.\n" +
                        "If SEED is specified, then it's used for both - sample creation and input generation.\n",
                opts,
                ""
        );
    }

    static InputStream getRandomInputStream(Long size, Random rnd) {
        Supplier<Byte> randomSupplier = () -> (byte) ALPHABET.charAt(rnd.nextInt(ALPHABET.length()));
        final Byte[] randomBytes = Stream.generate(randomSupplier)
                .limit(size)
                .toArray(Byte[]::new);

        return new ByteArrayInputStream(ArrayUtils.toPrimitive(randomBytes));
    }

    static class Sampler {

        /**
         * Create a random representative of a stream.
         * @param in input stream
         * @param out output stream where sample will be written to
         * @param sampleSize size of a sample
         * @param rnd instance of {@link Random} class used to generate sample
         */
        static void sampleStream(InputStream in, PrintStream out, Integer sampleSize, Random rnd) {
            Map<Byte, Long> counters = readInputStream(new BufferedInputStream(in));

            Long streamSize = counters.values().stream().reduce(0L, (a, b) -> a + b);

            List<Long> indexes = IntStream.range(0, sampleSize)
                    .mapToLong((x) -> (long) (streamSize * rnd.nextDouble()))
                    .boxed()
                    .collect(Collectors.toList());

            getRepresentatives(counters, indexes).stream()
                    .filter(Objects::nonNull)
                    .map(b -> (char) b.byteValue())
                    .forEach(out::print);
        }

        static Map<Byte, Long> readInputStream(InputStream s) {
            Map<Byte, Long> counters = new HashMap<>();
            byte EOS = (byte) -1;
            try {
                Byte consumedByte = (byte) s.read();
                while (consumedByte != EOS) {
                    counters.merge(consumedByte, 1L, (old, n) -> old + 1);
                    consumedByte = (byte) s.read();
                }

            } catch (IOException e) {
                throw new RuntimeException("Error while reading a stream", e);
            }
            return counters;
        }

        static List<Byte> getRepresentatives(Map<Byte, Long> inputCounters, List<Long> representatives) {
            final Long[] currentCount = new Long[1];
            currentCount[0] = 0L;
            List<Byte> result = Arrays.asList(new Byte[representatives.size()]);

            inputCounters.entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .forEach(entry -> {
                        for (int i = 0; i < representatives.size(); i++) {
                            Long rep = representatives.get(i);
                            if (rep >= currentCount[0] && rep < currentCount[0] + entry.getValue()) {
                                result.set(i, entry.getKey());
                            }
                        }
                        currentCount[0] += entry.getValue();

                    });

            return result;
        }
    }
}
