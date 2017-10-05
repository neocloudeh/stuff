import org.joda.time._
import org.joda.time.format.DateTimeFormat

/**
  * Created by neocloudeh on 13/05/2017.
  */
object Taxcercise extends App {
  val contractDayRateL = Seq(450)
  val timeUnemployedL = Seq(90)
  val professionalIndemnityInsurance = BigDecimal(1000)


  val VAT = 0.2
  val VAT_COEF = BigDecimal(1) + VAT
  val VAT_PAY_BACK = 0.145
  val FINAL_VAT_COEF = BigDecimal(1) + (VAT - VAT_PAY_BACK)

  val PII_PER_DAY = professionalIndemnityInsurance / 365.5

  val oneMonth = 23

  val bonusAmountAfterTax = BigDecimal(14393.76)

  val today = LocalDate.now()

  val bonusDay = new LocalDate(2018, 4, 30)

  val deadline = bonusDay.plusMonths(3)

  val daysBetween = Days.daysBetween(today, bonusDay.plusDays(3)).getDays

  val handInNoticeL = (0 to daysBetween by 7).map(today.plusDays)

  val formatter = DateTimeFormat.forPattern("dd/MM/yyyy")

  val holidays: Set[LocalDate] = Set("28/08/2017",	"25/12/2017",
    "26/12/2017",	"01/01/2018",	"30/03/2018",
    "02/04/2018",	"07/05/2018", "28/05/2018").map(formatter.parseLocalDate)

  val weekend = Set(6, 7)

  def isWorkingDay(day: LocalDate) = {
    !weekend.contains(day.getDayOfWeek()) && !holidays.contains(day)
    true
  }

  val lastDayOf2017 = new LocalDate(2017, 12, 31)
  val firstDayOf2018 = new LocalDate(2017, 12, 31)

  def workingDays: Map[LocalDate, Int] = (-60 to (Days.daysBetween(today, deadline).getDays + 60)).map(today.plusDays).
    reverse.filter(isWorkingDay).zipWithIndex.toMap

  def workingDaysBetween(start: LocalDate, end: LocalDate) = if(start.isAfter(end)) 0 else {
    workingDays(start) - workingDays(end)
  }

  val salary2017 = (BigDecimal(3603.76) * 12) / 365.5

  val salary2018 = (BigDecimal(4016.99) * 12) / 365.5



  implicit class RichJoda(val x: LocalDate) extends AnyVal{
    def max(y: LocalDate): LocalDate = if(x.isAfter(y)) x else y
    def min(y: LocalDate): LocalDate = if(x.isBefore(y)) x else y
  }

  def calculateNetIncome(rawProfits: BigDecimal):BigDecimal = {
    //See http://www.contractorcalculator.co.uk/salary_versus_dividends_limited_companies_advice.aspx and fix
    val heuristic = BigDecimal(0.8073)

    val niThreshold = BigDecimal(8060)
    val profits = rawProfits - niThreshold

    val corpTaxCoef = BigDecimal(0.81)
    val totalAvailableForDividends = profits * corpTaxCoef

    //todo: Do this as a fold with the highest brackets first in the list
    val bracketsAndRates =
      List(
        150000 -> 0.381,
        32000 -> 0.325,
        5000 -> 0.075
      )

    val start = (profits, BigDecimal(0))

    val (untaxed, taxed) = bracketsAndRates.foldLeft(start){
      case ((profits, paidOut), (bracket, taxRate)) =>
        val toTax = (profits - bracket).max(0)
        val remaining = profits - toTax
        (remaining, paidOut + (toTax * (BigDecimal(1) - taxRate)))
    }

    val total = untaxed + taxed

    total + niThreshold
  }

  val totals = for {
    handInNotice <- handInNoticeL
    timeUnemployed <- timeUnemployedL
    dayRate <- contractDayRateL
    lastDay = handInNotice.plusMonths(3)
    income2017 = Math.max(workingDaysBetween(today, lastDay.min(lastDayOf2017)), 0) * salary2017
    income2018 = Math.max(workingDaysBetween(firstDayOf2018, lastDay), 0) * salary2018
    bonus = if(!handInNotice.isBefore(bonusDay)) bonusAmountAfterTax else BigDecimal(0)
    totalWorkIncome = income2017 + income2018 + bonus
    startContracting = lastDay.plusDays(timeUnemployed)
    contractingDays = workingDaysBetween(startContracting, deadline)
    contractIncome = contractingDays * dayRate
    accountantCost = (BigDecimal(contractingDays) / oneMonth).setScale(0, BigDecimal.RoundingMode.UP) * (80 * VAT_COEF)
    incorporationCost = 90 * VAT_COEF
    insurance = PII_PER_DAY * contractingDays
    costs = accountantCost + incorporationCost + insurance
    contractTotal = calculateNetIncome((contractIncome * FINAL_VAT_COEF) - costs)
    total = totalWorkIncome + contractTotal
  } yield handInNotice -> total.setScale(0, BigDecimal.RoundingMode.DOWN)

  val bonusVal = totals.dropWhile(_._1.isBefore(bonusDay)).head._2

  totals.takeWhile(_._2 >= bonusVal).lastOption.fold(println("you're already too late...")){x =>
    val (thresholdDate, money) = x
    println(s"Hand in notice before ${thresholdDate.toString(DateTimeFormat.longDate())} " +
      s"for £$money or more otherwise wait until bonus\n\n")
  }

}
