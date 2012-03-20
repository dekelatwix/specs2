package user
package specification

class UserFromSourceSpecification extends org.specs2.Specification { def is = noindent^
                                                                                      p^
  "this is a one-liner"                                                               ^
  { 1 must_== 1 }                                                                     ^
  "this is an example on several lines"                                               ^
  { val a = "hello"
    val b = " world"
    (a + b) must_== "hello world"
  }                                                                                   ^
  `a call to an example`                                                              ^
                                                                                      end

  def `a call to an example` = success
}

class DifferentSpecification extends org.specs2.Specification { def is = noindent^
  "first example" ^
  { 1 must_== 1 } ^
                  end
}

class SpecificationWithNoStartingText extends org.specs2.Specification { def is =
  { 1 must_== 1 } ^
  { 2 must_== 2 } ^
  { 3 must_== 3 } ^
                  end
}

class SpecificationWithNoStartingTextAndNoEnd extends org.specs2.Specification { def is =
  { 1 must_== 1 } ^
  { 2 must_== 2 } ^
  { 3 must_== 3 }
}

class MutableSpecificationAutoExamples extends org.specs2.mutable.Specification {

  { 1 === 1 }.eg

  { 2 === 2 }.eg;
  { 3 === 3 }.eg

  `an example`.eg

  // this code can't be found at the moment
  eg {
    val i = 1
    i === 1
  }

  def `an example` = 4 === 4

}

