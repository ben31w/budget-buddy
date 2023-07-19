# budget-buddy
- Input: an Excel workbook that lists my expenses and deposits
- Output: pie charts that summarize my financial habits, breaking down my expenses 
and deposits by user-defined categories (restaurant, groceries, gas, car, etc.)

Each sheet in the workbook indicates a month of expenses and deposits. Two pie 
charts will be created for each sheet/month: one for expenses and one for deposits.


## Current Dependencies
- XChart 3.8.2 : used to create the pie charts (https://knowm.org/open-source/xchart/)
- Apache POI 5.2.3 : used to read Excel spreadsheets (https://poi.apache.org/)
- JavaFX 17.0.7 : used for frontend GUI (https://openjfx.io/)

These dependecies are not included in this repo.


## Extensions/TODOs
- Need better error handling if the Excel file isn't formatted properly.
- Add a 'loading' image to display while the workbook is being processed.
- Display the PieCharts inside BudgetBuddy.
- Maybe allow the user to select an output path to save their images.
- Clear output directory before saving the pie charts.
- Use Gradle to manage dependencies instead of IntelliJ.