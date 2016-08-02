import scala.util.Random

object PruneMatcher {
  type Score = Int
  type RankedPlayer = Int
  type WeightedPairing = (RankedPlayer, RankedPlayer, Int)
  type Combination = (Score, List[(RankedPlayer, RankedPlayer)])

  def f (x: List[WeightedPairing], cur: Combination, best: Combination, remaining_pairs: Int) : Combination = {
    if (remaining_pairs == 0) cur
    else {
      val (_, _, t) = (x take remaining_pairs).unzip3
      if (t.size < remaining_pairs || t.sum + cur._1 >= best._1) best
      else {
        val p = x.head
        val include_p = f ( x.tail.filter { q => p._1 != q._1 && p._1 != q._2 && p._2 != q._1 && p._2 != q._2 },
                           (cur._1 + p._3, (p._1, p._2) :: cur._2),
                           best,
                           remaining_pairs - 1)
        // exclude_p
        f (x.tail, cur, include_p, remaining_pairs)
      }
    }
  }
  def minWeightMatching (edges: List[(Int, Int, Int)]) : Array[Int] = {
    val nvertex = 1 + edges.map ( x => x._1.max (x._2) ).max
    assert((nvertex % 2) == 0)
    val c = f(edges.sortWith((a, b) => a._3 < b._3), (0, Nil), (Int.MaxValue, Nil), nvertex >> 1)
    val a: Array[Int] = Array.fill(nvertex)(-1)
    for ((u, v) <- c._2) {
      assert (u != v)
      assert (u >= 0 && u < nvertex)
      assert (v >= 0 && v < nvertex)
      assert (a(u) == -1 && a(v) == -1)
      a(u) = v
      a(v) = u
    }
    a
  }
}

object Checker {
  def result(edges: List[(Int, Int, Int)], a: Array[Int], s: Int): Int = {
    edges match {
      case Nil => s
      case x :: xs => result (xs, a, s + (if (a(x._1) == x._2) x._3 else 0))
    }
  }
  def edgesRandomShuffle(edges: List[(Int, Int, Int)], r: Random): List[(Int, Int, Int)] = {
    val a = r.shuffle(edges).toArray
    var n = a.length
    for (i <- 0 until n) {
      if (r.nextBoolean()) {
        a(i) = (a(i)._2, a(i)._1, a(i)._3)
      }
    }
    a.toList
  }
  def check(edges: List[(Int, Int, Int)], a: Array[Int], b: Array[Int], sa: String, sb: String ) = {
    assert (a.length == b.length)
    val ra = result(edges, a, 0)
    printf ("%s solution %d, %s\n", sa, ra, a.toList)
    val rb = result(edges, b, 0)
    printf ("%s solution %d, %s\n", sb, rb, b.toList)
    assert(ra == rb)
  }
}

object TestWMMatching extends App {
  val benchmark = 0
  def test(n: Int, min_weight: Int, max_weight: Int, seed: Int) {
    printf("test(n: %d, min_weight: %d, max_weight: %d, seed: %d)\n", n, min_weight, max_weight, seed)
    val r = new Random(seed)
    val edges = WMMatching.fullGraph(n, (i, j) => r.nextInt(max_weight-min_weight)+min_weight)
    //println (edges)
    val (sa, a) =
      if (n <= 20) ("prune", PruneMatcher.minWeightMatching(edges))
      else ("shuffle", WMMatching.minWeightMatching(Checker.edgesRandomShuffle(edges, r)))
    val b = WMMatching.minWeightMatching(edges)
    Checker.check(edges, a, b, sa, "matcher")
  }
  def testZeroMatching(n: Int, seed: Int) {
    printf("testZeroMatching(n: %d, seed: %d)\n", n, seed)
    val r = new Random(seed)
    val mate = Array.fill(n)(-1)
    for (List(u,v) <- r.shuffle((0 until n).toList).grouped(2)) {
      assert(mate(u) < 0 && mate(v) < 0)
      mate(u) = v
      mate(v) = u
    }
    val edges = WMMatching.fullGraph(n, ((i,j) => if (mate(i) == j) 0 else 1))
    val a = WMMatching.minWeightMatching (edges)
    val ra = Checker.result(edges, a, 0)
    assert(ra == 0)
  }


  def handTests() : Unit = {
    def check(edges:List[(Int,Int,Int)], maxcardinality: Boolean, expected: List[Int]) : Unit = {
      assert(WMMatching.maxWeightMatching(edges, maxcardinality).toList == expected)
    }

    check(List((0,1,1)), false, List (1, 0))
    check(List((1,2,10), (2,3,11)), false, List( -1, -1, 3, 2 ))
    check(List((1,2,5), (2,3,11), (3,4,5)), false, List( -1, -1, 3, 2, -1))
    check(List((1,2,5), (2,3,11), (3,4,5)), true, List(-1, 2, 1, 4, 3))
    println("create S-blossom and use it for augmentation")
    check(List((1,2,8), (1,3,9), (2,3,10), (3,4,7)), false, List( -1, 2, 1, 4, 3 ))
    check(List((1,2,8), (1,3,9), (2,3,10), (3,4,7), (1,6,5), (4,5,6)), false, List( -1, 6, 3, 2, 5, 4, 1))
    println("create S-blossom, relabel as T-blossom, use for augmentation")
    check(List( (1,2,9), (1,3,8), (2,3,10), (1,4,5), (4,5,4), (1,6,3) ), false, List( -1, 6, 3, 2, 5, 4, 1 ))
    check(List( (1,2,9), (1,3,8), (2,3,10), (1,4,5), (4,5,3), (1,6,4) ), false, List( -1, 6, 3, 2, 5, 4, 1 ))
    check(List( (1,2,9), (1,3,8), (2,3,10), (1,4,5), (4,5,3), (3,6,4) ), false, List( -1, 2, 1, 6, 5, 4, 3 ))
    println("create nested S-blossom, use for augmentation")
    check(List( (1,2,9), (1,3,9), (2,3,10), (2,4,8), (3,5,8), (4,5,10), (5,6,6) ), false, List( -1, 3, 4, 1, 2, 6, 5 ))
    println("create S-blossom, relabel as S, include in nested S-blossom")
    check(List( (1,2,10), (1,7,10), (2,3,12), (3,4,20), (3,5,20), (4,5,25), (5,6,10), (6,7,10), (7,8,8) ), false, List( -1, 2, 1, 4, 3, 6, 5, 8, 7 ))
    println("create nested S-blossom, augment, expand recursively")
    check(List( (1,2,8), (1,3,8), (2,3,10), (2,4,12), (3,5,12), (4,5,14), (4,6,12), (5,7,12), (6,7,14), (7,8,12) ), false, List( -1, 2, 1, 5, 6, 3, 4, 8, 7 ))
    println("create S-blossom, relabel as T, expand")
    check(List( (1,2,23), (1,5,22), (1,6,15), (2,3,25), (3,4,22), (4,5,25), (4,8,14), (5,7,13) ), false, List( -1, 6, 3, 2, 8, 7, 1, 5, 4 ))
    println("create nested S-blossom, relabel as T, expand")
    check(List( (1,2,19), (1,3,20), (1,8,8), (2,3,25), (2,4,18), (3,5,18), (4,5,13), (4,7,7), (5,6,7) ), false, List( -1, 8, 3, 2, 7, 6, 5, 4, 1 ))
    println("create blossom, relabel as T in more than one way, expand, augment")
    check(List( (1,2,45), (1,5,45), (2,3,50), (3,4,45), (4,5,50), (1,6,30), (3,9,35), (4,8,35), (5,7,26), (9,10,5) ), false, List( -1, 6, 3, 2, 8, 7, 1, 5, 4, 10, 9 ))
    println("again but slightly different")
    check(List( (1,2,45), (1,5,45), (2,3,50), (3,4,45), (4,5,50), (1,6,30), (3,9,35), (4,8,26), (5,7,40), (9,10,5) ), false, List( -1, 6, 3, 2, 8, 7, 1, 5, 4, 10, 9 ))
    // create blossom, relabel as T, expand such that a new least-slack S-to-free edge is produced, augment
    check(List( (1,2,45), (1,5,45), (2,3,50), (3,4,45), (4,5,50), (1,6,30), (3,9,35), (4,8,28), (5,7,26), (9,10,5) ), false, List( -1, 6, 3, 2, 8, 7, 1, 5, 4, 10, 9 ))
    println("create nested blossom, relabel as T in more than one way, expand outer blossom such that inner blossom ends up on an augmenting path")
    check(List( (1,2,45), (1,7,45), (2,3,50), (3,4,45), (4,5,95), (4,6,94), (5,6,94), (6,7,50), (1,8,30), (3,11,35), (5,9,36), (7,10,26), (11,12,5) ), false, List( -1, 8, 3, 2, 6, 9, 4, 10, 1, 5, 7, 12, 11 ))
    println("create nested S-blossom, relabel as S, expand recursively")
    check(List( (1,2,40), (1,3,40), (2,3,60), (2,4,55), (3,5,55), (4,5,50), (1,8,15), (5,7,30), (7,6,10), (8,10,10), (4,9,30) ), false, List( -1, 2, 1, 5, 9, 3, 7, 6, 10, 4, 8 ))
  }

  if (benchmark > 0) {
    test(300, 1, 10000, 2)
  } else {
    handTests()
    var seed = 1
    val iterations = 1000
    for (n <- 2 to 20 by 2 ;
         t <- List(10, 20, 50, 100, 200, 500, 1000, 10000);
         i <- 1 to iterations) {
      test(n, 1, t, seed)
      seed += 1
    }

    for (n <- 20 to 200 by 10;
         t <- List(10, 20, 50, 100, 200, 500, 1000, 10000))  {
      test(n, 1, t, seed)
      seed += 1
      testZeroMatching(n, seed)
      seed += 1
    }
  }
}
