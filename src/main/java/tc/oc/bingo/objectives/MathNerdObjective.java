package tc.oc.bingo.objectives;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.channels.GlobalChannel;

@Tracker("math-nerd")
public class MathNerdObjective extends ObjectiveTracker {

  private final Supplier<Integer> QUIZ_INTERVAL = useConfig("quiz-interval", 120);
  private Double currentAnswer = null;
  private Future<?> task = null;

  private Match match;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    match = event.getMatch();
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    queueQuiz();
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    cleanupQuiz();
  }

  private void queueQuiz() {
    cleanupQuiz();
    task =
        PGM.get()
            .getExecutor()
            .scheduleWithFixedDelay(
                this::postMathQuestion, QUIZ_INTERVAL.get(), QUIZ_INTERVAL.get(), TimeUnit.SECONDS);
  }

  private void cleanupQuiz() {
    if (task != null) {
      task.cancel(true);
      task = null;
    }
  }

  private void postMathQuestion() {
    double min = 0.25;
    double max = 2.75;
    double exponent = 2.0; // Adjust for skew
    double weight = 0.6; // Blend factor between linear and power weighting

    // Generate a weighted random number
    double uniform = Math.random(); // Linear distribution
    double skewed = Math.pow(Math.random(), exponent); // Power function for skew
    double blended = (weight * uniform) + ((1 - weight) * skewed); // Blend them

    double randomValue = min + (blended * (max - min));

    var node = generateGoodNode(randomValue);

    // Build [Tip] Solve this x component
    TextComponent.Builder tip =
        Component.text()
            .append(Component.text("[", NamedTextColor.GRAY, TextDecoration.BOLD))
            .append(Component.text("Tip", NamedTextColor.BLUE, TextDecoration.BOLD))
            .append(Component.text("]", NamedTextColor.GRAY, TextDecoration.BOLD))
            .append(Component.text(" Solve this: ", NamedTextColor.DARK_AQUA))
            .append(Component.text(node.asString(), NamedTextColor.GOLD));
    match.sendMessage(tip);

    this.currentAnswer = node.result();
  }

  @EventHandler
  public void onPlayerChat(ChannelMessageEvent<?> event) {
    if (currentAnswer == null || !(event.getChannel() instanceof GlobalChannel)) return;

    String trim = event.getMessage().trim();
    String[] split = trim.split(" ");
    try {
      double answer = Double.parseDouble(split[0]);
      if (answer == currentAnswer) {
        reward(event.getSender().getBukkit());

        // Immediately start the next question
        currentAnswer = null;
        queueQuiz();
      }
    } catch (NumberFormatException ignored) {
    }
  }

  private static final Literal[] LITERALS =
      IntStream.rangeClosed(0, 100).mapToObj(Literal::new).toArray(Literal[]::new);
  private static final int BAD = 0, SMALL = 1, BIG = 2;
  private static final Random random = new Random();
  private static final Set<Operation> INITIAL_BAN = Set.of(Operation.POW, Operation.FACTORIAL);

  public Node generateGoodNode(double complexity) {
    var fails = new int[3];
    for (int i = 0; i < 100; i++) {
      Node node = tryGenerateNode(complexity, fails);
      if (node != null) return node;
    }
    double nextAttempt = complexity < 1d ? complexity + 0.05 : complexity - 0.05d;
    return generateGoodNode(nextAttempt);
  }

  private Node tryGenerateNode(double complexity, int[] fails) {
    Node node = generateNode(complexity, INITIAL_BAN);
    String formula = node.asString();

    var result = Math.abs(node.result());
    if (result <= 1 || result >= 10_000 || result % 0.25d != 0) {
      fails[BAD]++;
      return null;
    }
    // 0.25: 4..16
    // 0.50: 4..18
    // 0.75: 6..27
    // 1.00: 8..36
    // 1.25: 10..45
    // 1.50: 12..54
    // 1.75: 12..63
    // 2.00: 12..72
    if (formula.length() < Math.clamp((int) Math.ceil(complexity * 8), 4, 12)) {
      fails[SMALL]++;
      return null;
    }
    if (formula.length() > Math.clamp((int) Math.ceil(complexity * 36), 16, 72)) {
      fails[BIG]++;
      return null;
    }
    return node;
  }

  private Node generateNode(final double complexity, Set<Operation> skip) {
    if (INITIAL_BAN != skip
        && (complexity <= 0.15d || (complexity < 0.4d && Math.random() > (complexity * 2)))) {
      // 9% to 90% of big number, for complexities 0.03 to 0.3
      boolean big = Math.random() < Math.clamp(complexity, 0.03, 0.3) * 3;
      return Literal.of(2 + random.nextInt(big ? 99 : 9));
    }
    Operation op = Operation.pickRandom(complexity);
    while (skip.contains(op)) op = Operation.pickRandom(complexity);
    int count = Math.min(2, op.maxChildren);
    // Add additional children randomly
    while (count < op.maxChildren && Math.random() < (complexity - (op.complexity * count)))
      count++;
    Node[] children = new Node[count];
    double childComplexity = complexity - (op.complexity * count);

    for (int attempt = 0; attempt < 6; attempt++) {
      for (int i = 0; i < count; i++) {
        children[i] = generateNode(childComplexity, op.set);
      }
      var exp = new Expression(op, children);
      var result = Math.abs(exp.result());
      // If node is bad, try again with same operation
      if (result > 1 && result < 10_000 && (attempt >= 4 || result % 0.25d == 0)) {
        return exp;
      }
    }
    // It was too hard to find a good set of children for this op
    return generateNode(complexity, skip);
  }

  @RequiredArgsConstructor
  private enum Operation {
    PLUS("+", Double::sum, MathNerdObjective::sum, 5, 0.15d, 100),
    MINUS("-", (a, b) -> a - b, MathNerdObjective::sub, 5, 0.2d, 80),
    MULTIPLY("*", (a, b) -> a * b, MathNerdObjective::mul, 3, 0.3d, 70),
    DIVIDE("/", (a, b) -> a / b, 0.3d, 50),
    POW("^", Math::pow, 0.5d, 20),
    FACTORIAL("!", MathNerdObjective::fac, 0.9d, 15);

    private static final Operation[] VALUES = Operation.values();
    private static final double[] weights = new double[VALUES.length];
    private static double totalWeight = 0, lastComplexity = 0;

    private final String symbol;
    private final DoubleUnaryOperator unary;
    private final DoubleBinaryOperator binary;
    private final ToDoubleFunction<double[]> unbound;
    private final int maxChildren;
    private final double complexity;
    private final int weight;
    private final Set<Operation> set = Set.of(this);

    // Purely unary operation, eg: factorial
    Operation(String symbol, DoubleUnaryOperator unary, double complexity, int weight) {
      this(symbol, unary, null, null, 1, complexity, weight);
    }

    // Purely binary operation, eg: pow, divide
    Operation(String symbol, DoubleBinaryOperator binary, double complexity, int weight) {
      this(symbol, null, binary, null, 2, complexity, weight);
    }

    // Binary or higher operation, eg: plus, minus, mul
    Operation(
        String symbol,
        DoubleBinaryOperator binary,
        ToDoubleFunction<double[]> unbound,
        int maxChildren,
        double complexity,
        int weight) {
      this(symbol, null, binary, unbound, maxChildren, complexity, weight);
    }

    private void toString(StringBuilder out, Node... children) {
      if (maxChildren == 1) {
        children[0].write(out);
        out.append(" ").append(symbol).append(" ");
        return;
      }
      for (int i = 0; i < children.length - 1; i++) {
        children[i].write(out);
        out.append(" ").append(symbol).append(" ");
      }
      children[children.length - 1].write(out);
    }

    private double result(double val) {
      return unary.applyAsDouble(val);
    }

    private double result(double a, double b) {
      return binary.applyAsDouble(a, b);
    }

    private double result(double... vals) {
      return unbound.applyAsDouble(vals);
    }

    private static Operation pickRandom(double complexity) {
      // It picks a random operation, but as complexity increases the more complex operations are
      // more likely
      double shiftedComp = Math.max(complexity, 0.75) - 0.25;
      if (lastComplexity != shiftedComp) {
        lastComplexity = shiftedComp;
        totalWeight = 0;
        for (int i = 0; i < Operation.VALUES.length; i++) {
          var op = VALUES[i];
          double weight = op.weight * Math.max(0, shiftedComp - op.complexity);
          weights[i] = weight;
          totalWeight += weight;
        }
      }

      double toPick = random.nextDouble(totalWeight);
      for (int i = 0; i < weights.length; i++) {
        toPick -= weights[i];
        if (toPick <= 0) return VALUES[i];
      }
      // Should never happen, but add a fallback regardless
      return Math.random() > 0.4 ? PLUS : MINUS;
    }
  }

  private static double sum(double... vals) {
    double sum = 0;
    for (double val : vals) sum += val;
    return sum;
  }

  private static double sub(double... vals) {
    double sum = vals[0];
    for (int i = 1; i < vals.length; i++) {
      sum -= vals[i];
    }
    return sum;
  }

  private static double mul(double... vals) {
    double res = 1;
    for (double val : vals) res *= val;
    return res;
  }

  private static double fac(double val) {
    if (val <= 1 || (val % 1) != 0) return Double.NaN;
    double result = 1;
    for (int i = 2; i <= (long) val; i++) result *= i;
    return result;
  }

  public interface Node {
    void write(StringBuilder out);

    double result();

    default String asString() {
      var builder = new StringBuilder();
      write(builder);
      if (builder.charAt(0) != '(') return builder.toString();
      return builder.substring(1, builder.length() - 1);
    }
  }

  private record Expression(Operation operation, Node[] children) implements Node {
    @Override
    public void write(StringBuilder out) {
      out.ensureCapacity(out.length() + 2 + children.length * 3);
      out.append('(');
      operation.toString(out, children);
      out.append(')');
    }

    @Override
    public double result() {
      // Fast & allocation-less paths for unary and binary cases
      if (children.length == 1) return operation.result(children[0].result());
      if (children.length == 2) return operation.result(children[0].result(), children[1].result());

      var results = new double[children.length];
      for (int i = 0; i < children.length; i++) {
        results[i] = children[i].result();
      }
      return operation.result(results);
    }
  }

  private record Literal(int value) implements Node {
    public static Literal of(int value) {
      return value < LITERALS.length ? LITERALS[value] : new Literal(value);
    }

    @Override
    public void write(StringBuilder out) {
      out.append(value);
    }

    @Override
    public double result() {
      return value;
    }
  }

  /*
  // Debug code:
  public static void main(String[] args) {
    runAsserts();

    MathNerdObjective mathNerdObjective = new MathNerdObjective();
    for (int i = 0; i < 100; i++) {
      mathNerdObjective.postMathQuestion();
    }

    //    checkOperationOdds();
    //    checkGenerationRates(0.25, 2.75, 0.25, 10_000);
    generateExamples(0.25, 2.75, 0.25, 5);
  }

  private static void checkOperationOdds() {
    System.out.println("Chance of operation per complexity:");
    System.out.println("Complexity,SUM,SUB,MUL,DIV,POW,FACT,MAX_WEIGHT");
    for (double complexity = 0.1; complexity <= 3; complexity += 0.1) {
      System.out.printf("%4.2f,", complexity);

      Operation.pickRandom(complexity); // Update weights
      for (double weight : Operation.weights) {
        System.out.print(Math.round((weight / Operation.totalWeight) * 10000) / 100d + "%,");
      }
      System.out.printf("%4.2f\n", Operation.totalWeight);
    }
  }

  private static void checkGenerationRates(double min, double max, double step, int amount) {
    var obj = new MathNerdObjective();
    System.out.println("\n\nGeneration success rate per complexity:");
    System.out.println("Comp  Success    Bad  Short   Long PerAttempt PerSuccess");
    for (double complexity = min; complexity <= max + 0.0001; complexity += step) {
      int success = 0;
      var err = new int[3];
      double pct = 1 / (amount / 100d);
      long time = System.currentTimeMillis();
      for (int i = 0; i < amount; i++) {
        var node = obj.tryGenerateNode(complexity, err);
        if (node != null) success++;
      }
      time = System.currentTimeMillis() - time;
      double perAttempt = ((double) time / amount);
      double perSuccess = ((double) time / success);
      System.out.printf(
          "%.2f %7.3f%% %5.1f%% %5.1f%% %5.1f%% %8.5fms %8.5fms\n",
          complexity,
          success * pct,
          err[0] * pct,
          err[1] * pct,
          err[2] * pct,
          perAttempt,
          perSuccess);
    }
  }

  private static void generateExamples(double min, double max, double step, int amount) {
    var obj = new MathNerdObjective();
    System.out.println("\nExamples per complexity:");
    int[] errs = new int[3];
    for (double complexity = min; complexity <= max + 0.0001; complexity += step) {
      System.out.printf("\nComplexity: %.2f\n", complexity);
      int remaining = amount;
      for (int i = 0; i < 10_000 && remaining > 0; i++) {
        var node = obj.tryGenerateNode(complexity, errs);
        if (node == null) continue;
        System.out.println(node.asString() + " = " + node.result());
        remaining--;
      }
    }
  }

  private static void runAsserts() {
    var children = new Node[] {Literal.of(5), Literal.of(2)};

    if (new Expression(Operation.PLUS, children).result() != 7) throw new IllegalStateException();
    if (new Expression(Operation.MINUS, children).result() != 3) throw new IllegalStateException();
    if (new Expression(Operation.MULTIPLY, children).result() != 10)
      throw new IllegalStateException();
    if (new Expression(Operation.DIVIDE, children).result() != 2.5)
      throw new IllegalStateException();
    if (new Expression(Operation.POW, children).result() != 25) throw new IllegalStateException();
    if (new Expression(Operation.FACTORIAL, new Node[] {Literal.of(5)}).result() != 120)
      throw new IllegalStateException();
  }*/
}
