# budget-buddy
- Input: an Excel workbook that lists my expenses and deposits
- Output: pie charts that summarize my financial habits, breaking down my expenses 
and deposits by user-defined categories (restaurant, groceries, gas, car, etc.)

Each sheet in the workbook indicates a month of expenses and deposits. Two pie 
charts will be created for each sheet/month: one for expenses and one for deposits.


## Current Dependencies
- XChart 3.8.2 : used to create the pie charts (https://knowm.org/open-source/xchart/)
- Apache POI 5.2.3 : used to read Excel spreadsheets (https://poi.apache.org/)
These dependecies are not included in this repo.


## Extensions/TODOs
- Allow user to select the workbook to process rather than hardcoding the name of the file
- Implement a frontend in JavaFX that allows user to select a workbook, and displays the PieCharts in one window.