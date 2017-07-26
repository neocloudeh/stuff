/**
  * Created by neocloudeh on 26/07/2017.
  */
object RequiredOutgoings extends App {
  val savings = BigDecimal(14200)

  val allOutgoings = Map(
    'heating -> 45,
    'barclaycard -> 131,
    'broadband -> 19,
    'sainsburys -> 100,
    'EE -> 36,
    'Mortgage -> 513,
    'councilTax -> 80,
    'rent -> 680,
    'water -> 30,
    'electricity -> 40,
    'foodAndEntertainment -> 330
  )

  val total = allOutgoings.values.map(BigDecimal(_)).sum

  val months = (savings / total).setScale(1, BigDecimal.RoundingMode.DOWN)

  println(s"Total outgoings per month are $total and this will last for $months months")
}
