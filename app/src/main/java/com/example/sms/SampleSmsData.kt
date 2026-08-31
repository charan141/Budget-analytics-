package com.example.sms

data class SampleSmsItem(
    val bankName: String,
    val sender: String,
    val message: String,
    val expectedCategory: String,
    val expectedType: String
)

object SampleSmsData {
    val SAMPLES = listOf(
        SampleSmsItem(
            bankName = "Chase Bank",
            sender = "CHASE-ALERT",
            message = "Chase Alert: Your card ending in 4128 was charged $42.50 at STARBUCKS COFFEE on 08/30/2026. Available credit $4,850.00.",
            expectedCategory = "Food & Dining",
            expectedType = "EXPENSE"
        ),
        SampleSmsItem(
            bankName = "HDFC Bank UPI",
            sender = "HDFCBK",
            message = "Dear Customer, INR 1,450.00 has been debited from account **5678 to Swiggy UPI on 30-AUG-26. Avl Bal: INR 45,210.00.",
            expectedCategory = "Food & Dining",
            expectedType = "EXPENSE"
        ),
        SampleSmsItem(
            bankName = "Wells Fargo",
            sender = "WELLSFARGO",
            message = "Wells Fargo: $148.90 debited from checking a/c ...9012 for Whole Foods Supermarket purchase.",
            expectedCategory = "Groceries",
            expectedType = "EXPENSE"
        ),
        SampleSmsItem(
            bankName = "Citibank Card",
            sender = "CITIBK",
            message = "Alert: USD 219.00 spent on Citi Credit Card ending 7731 at AMAZON PRIME RETAIL on 31-AUG.",
            expectedCategory = "Shopping",
            expectedType = "EXPENSE"
        ),
        SampleSmsItem(
            bankName = "Uber / Transit",
            sender = "GPAY",
            message = "Paid USD 34.20 to Uber Rides using Google Pay UPI ref 892341. Thank you!",
            expectedCategory = "Transport & Fuel",
            expectedType = "EXPENSE"
        ),
        SampleSmsItem(
            bankName = "Direct Deposit Salary",
            sender = "PAYROLL",
            message = "Direct Deposit: Account **3421 has been credited with USD 4,800.00 for MONTHLY SALARY / PAYROLL.",
            expectedCategory = "Salary",
            expectedType = "INCOME"
        ),
        SampleSmsItem(
            bankName = "Vanguard SIP Investment",
            sender = "VANGUARD",
            message = "Confirmation: Your auto-debit SIP investment of USD 400.00 into S&P 500 Index Mutual Fund executed successfully.",
            expectedCategory = "Mutual Funds / SIP",
            expectedType = "INVESTMENT"
        ),
        SampleSmsItem(
            bankName = "Utility Electric Bill",
            sender = "PWRUTIL",
            message = "Payment of $82.40 to Power Grid Utility was successful from account xx4419.",
            expectedCategory = "Bills & Utilities",
            expectedType = "EXPENSE"
        ),
        SampleSmsItem(
            bankName = "Netflix Subscription",
            sender = "AMEX",
            message = "Amex Alert: Card **1004 charged $19.99 at NETFLIX STREAMING SERVICES on 28-AUG.",
            expectedCategory = "Entertainment",
            expectedType = "EXPENSE"
        ),
        SampleSmsItem(
            bankName = "CVS Pharmacy",
            sender = "BOA",
            message = "Bank of America: Debit card 6620 spent $38.75 at CVS PHARMACY & MEDICAL on 29-AUG.",
            expectedCategory = "Health & Medical",
            expectedType = "EXPENSE"
        )
    )
}
