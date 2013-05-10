SquantLib
=========

*SquantLib* is a financial engineering tool focused on pricing Bonds written in Scala.

**Helps, suggestions and bug-fix are very much appreciated!!**

## What can it do?
###Rates
* Building discountable yield curves from
  * cash rate, swap rate, ccy basis swap and 3m/6m basis swap
  * non-deliverable swap
  * fx swap points
* Draw simple charts
* Compute cashflow discount factor using given discounting currency & spread.
* Build swaption volatility surface
* Pricing simple swaptions

###FX
* Compute forward fx
* Build fx volatility surfaces

###Equity and Index
* Compute forward price
* Build volatility surfaces

###Bond
* Cashflows
  * Fixed rate
  * Binary
  * Sum of linear equations
  * Cap, floor
  * Forward
  * American knock-in
  * Issuer's early termination
  * Automatic trigger
* Greeks
  * rate delta, fx delta
  * rate vega, fx vega
  * yield (simple, compounding)
  * duration (effective, modified, macaulay)
  * convexity, etc.
* Analysis
  * FX exercise frontier
  * Rate exercise frontier
  * Forward bond price
* Handling bonds with published prices

###Historical
* Historical volatility
* Historical correlation
* Moving average, etc

###Exotic
* BS formula
* Montecarlo models
  * continuous dividend
  * discrete dividend + repo
* Model extension
  * Exercise frontier pre-computation for issuer's callability

###To Do (maybe)
* More sophisticated montecarlo model (smile, multi-factor)
* Bermudan swaptions
* Portfolio analysis

## What is this for?
* Main pricing backbone behind PFOL.IO project (http://pfol.io)
* Free for personal use - any interest?
 
## Contact
Masakatsu Wakayu (masakatsu.wakayu@imperial-ft.com)
Imperial Finance & Technology (http://www.imperial-ft.com)
