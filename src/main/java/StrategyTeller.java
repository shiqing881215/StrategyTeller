import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

public class StrategyTeller {

	// Define the variables

	// the minimum down percentage from top to bottom that 
	// trigger listen to buy
	// the deeper, the better
	private double minimumDownPercentage = 0.15;
	
	// the minimum up percentage of the gap 
	// between top and bottom that trigger buying
	private double upPercentageToBuy = 1.0/3.0;
	
	// the percentage near previous high that can trigger
	// selling rule
	private double nearTopPercentage;

	// The percentage down from the top that trigger the sell
	private double sellTriggerPercentage = 0.15;
	
	public StrategyTeller() {
		super();
	}

	public StrategyTeller(double minimumDownPercentage, double upPercentageToBuy, double nearTopPercentage,
			double sellTriggerPercentage) {
		super();
		this.minimumDownPercentage = minimumDownPercentage;
		this.upPercentageToBuy = upPercentageToBuy;
		this.nearTopPercentage = nearTopPercentage;
		this.sellTriggerPercentage = sellTriggerPercentage;
	}
	
	public void setMinimumDownPercentage(double minimumDownPercentage) {
		this.minimumDownPercentage = minimumDownPercentage;
	}

	public void setUpPercentageToBuy(double upPercentageToBuy) {
		this.upPercentageToBuy = upPercentageToBuy;
	}

	public void setNearTopPercentage(double nearTopPercentage) {
		this.nearTopPercentage = nearTopPercentage;
	}

	public void setSellTriggerPercentage(double sellTriggerPercentage) {
		this.sellTriggerPercentage = sellTriggerPercentage;
	}

	/*
	 * Result[0] is strategy profit
	 * Result[1] is holding profit
	 */
	public List<Double> doMagic(List<DateAndPrice> datesAndPrices) {
		List<Transaction> transactions = new ArrayList<StrategyTeller.Transaction>();
		DateAndPrice previousHigh = new DateAndPrice("INIT DATE", Double.MIN_VALUE);
		DateAndPrice previousLow = new DateAndPrice("INIT DATE", Double.MAX_VALUE);
		
		boolean alreadyBought = false;
		boolean profitTakingPending = false;
		double firstBuyPrice = Double.MIN_VALUE;
		
		for (DateAndPrice dateAndPrice : datesAndPrices) {
			Double price = dateAndPrice.getPrice();
			Double high = previousHigh.getPrice();
			Double low = previousLow.getPrice();
			
			if (price > high) {
				previousHigh = dateAndPrice;
				
				// If pass previous high and already bought
				// That means we need attention big drop and profit taking
				if (alreadyBought) {
					profitTakingPending = true;
				}
			}
			
			if (price < low) {
				previousLow = dateAndPrice;
			}
			
			double gap = high - low;
			
			// buy
			// between low and high && 
			// but above a certain percentage of the gap &&
			// close to low &&
			// gap is above minimumDownPercentage
			if (price > low && price < high && 
					price >= low + gap*upPercentageToBuy &&
					(high+low)/2 > price &&
					(high-low)/high > minimumDownPercentage && 
					!alreadyBought) {
				// Buy
				Transaction t = new Transaction();
				String buyReason = "High " + previousHigh.getDate() + " " + high + 
						" Low " + previousLow.getDate() + " " + low;
				t.buy(dateAndPrice, buyReason);
				transactions.add(t);
				
				alreadyBought = true;
				
				if (firstBuyPrice == Double.MIN_VALUE) {
					firstBuyPrice = price;
				}
				
				continue;
			}
			
			// after buy, if it drop below previous low
			// trigger sell
			if (price < low && alreadyBought) {
				Transaction lastTransaction = transactions.get(transactions.size()-1);
				lastTransaction.sell(dateAndPrice, "Stop Loss");
				// TODO do i need set it back to transactions ?
				 
				
				// TODO Reset some variable
				alreadyBought = false;
				// only need to override previous low 
				previousLow = dateAndPrice;
				
				continue;
			}
			
			// If it's in profit taking mode
			// And it's down from high to a certain percentage
			// Take the profit
			if (profitTakingPending) {
				if ((high - price) / high > sellTriggerPercentage) {
					Transaction lastTransaction = transactions.get(transactions.size()-1);
					lastTransaction.sell(dateAndPrice, "Profit Taking");
					// TODO do i need set it back to transactions ?
					
					// Reset some variable
					alreadyBought = false;
					profitTakingPending = false;
					
					// Also set the sell point as new previous low
					// Forget the long time ago previous low
					previousLow = dateAndPrice;
					
					continue;
				}
			}
				
		}
		
		Double lastDayPrice = datesAndPrices.get(datesAndPrices.size()-1).getPrice();
		
		printTransaction(transactions, lastDayPrice);
		
		List<Double> profits = Lists.newArrayList();
		
		profits.add(calculateProfit(transactions, lastDayPrice));
		profits.add(calculateHoldingProfit(firstBuyPrice, lastDayPrice));
		
		return profits;
	}
	
	private double calculateProfit(List<Transaction> transactions, Double lastDayPrice) {
		System.out.println("Calculating strategy profit ......");
		double profit = 0;
		
		for (Transaction t : transactions) {
			profit += t.getSell(lastDayPrice).getPrice() - t.getBuy().getPrice();
		}
		
		return profit;
	}
	
	// Calculate the profit if holding from first buy day to the last day of the data
	private double calculateHoldingProfit(Double firstBuyPrice, Double lastDayPrice) {
		System.out.println("Calculating holding profit ......");
		return lastDayPrice - firstBuyPrice;
	}
	
	private void printTransaction(List<Transaction> transactions, Double lastDayPrice) {
		System.out.println("There are " + transactions.size() + " transaction happened in total");
		for (Transaction t : transactions) {
			System.out.println("Buy date : " + t.getBuy().getDate() + " | Buy Price : " + t.getBuy().getPrice() 
					+ " | Buy Reason : " + t.getBuyReason());
			System.out.println("Sell date : " + t.getSell(lastDayPrice).getDate() + " | Sell Price : " + t.getSell(lastDayPrice).getPrice() 
					+ " | Sell Reason : " + t.getSellReason());
		}
	}

	private class Transaction {
		private List<DateAndPrice> store;
		private String buyReason;  // TODO Add this
		private String sellReason;
		
		public Transaction() {
			store = new ArrayList<DateAndPrice>(2);
		}
		
		public Transaction(DateAndPrice buy, DateAndPrice sell) {
			store = new ArrayList<DateAndPrice>(2);
			store.add(buy);
			store.add(sell);
		}
		
		public void buy(DateAndPrice buy, String buyReason) {
			if (store == null) {
				store = new ArrayList<DateAndPrice>(2);
			}
			// add buy as the first element
			store.add(buy);
			this.buyReason = buyReason;
		}
		
		public void sell(DateAndPrice sell, String sellReason) {
			if (store == null) {
				store = new ArrayList<DateAndPrice>(2);
			}
			// add sell as the second element
			store.add(sell);
			this.sellReason = sellReason;
		}

		public DateAndPrice getBuy() {
			return store.get(0);
		}

		public DateAndPrice getSell(Double...lastDayPrice) {
			// The last one could potentially only buy, no sell
			// To calculate the profit, assume the last day of raw data as sell
			if (store.size() < 2) {
//				System.out.println("NO SELL");
				return new DateAndPrice("NO SELL DATE", lastDayPrice[0]); 
			}
			return store.get(1);
		}
		
		public String getSellReason() {
			return this.sellReason;
		}
		
		public String getBuyReason() {
			return this.buyReason;
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		FileReader reader = new FileReader();
		final String filePath = "/Users/qshi/Downloads/TQQQ.xlsx";
		
		List<DateAndPrice> rawData = reader.readFile(filePath);
		System.out.println("There are total " + rawData.size() + " trading days");
		
		/*
		StrategyTeller teller = new StrategyTeller();
		
//		final String filePath = "/Users/qshi/Downloads/HistoricalQuotes.xlsx";
		
		List<Double> profits = teller.doMagic(reader.readFile(filePath));
		
		System.out.println("Strategy profit : " + profits.get(0));
		System.out.println("Holding profit : " + profits.get(1));
		*/
		
		
		/*
		// Find the best combination of minimumDownPercentage & sellTriggerPercentage & upPercentageToBuy
		// Default is 15% 1/3 15%
		StrategyTeller teller = new StrategyTeller();
		Double bestStrategyProfit = Double.MIN_VALUE;
		List<Double> bestParams = Lists.newArrayList();
		Double holdingProfit = 0.0;
		
		for (double i = 0.05; i <= 0.5; i+=0.01) { // minimumDownPercentage
			for (double j = 0.05; j <= 0.5; j+=0.01) { // sellTriggerPercentage
				for (double k = 0.1; k <= 0.5; k+=0.01) { // upPercentageToBuy
					teller.setMinimumDownPercentage(i);
					teller.setSellTriggerPercentage(j);
					teller.setUpPercentageToBuy(k);
					
					List<Double> profits = teller.doMagic(reader.readFile(filePath));
					if (profits.get(0) > bestStrategyProfit) {
						bestStrategyProfit = profits.get(0);
						bestParams.clear();
						bestParams.add(i);
						bestParams.add(j);
						bestParams.add(k);
						holdingProfit = profits.get(1);
					}
				}
			}
		}
		
		System.out.println("-----------------------------------");
		System.out.println("Best params : minimumDownPercentage : " + bestParams.get(0) 
			+ " sellTriggerPercentage " + bestParams.get(1)
			+ " upPercentageToBuy" + bestParams.get(2) );
		System.out.println("Best strategy profit : " + bestStrategyProfit);
		System.out.println("Holding profit : " + holdingProfit);
		
		System.out.println("----------------------------------- best case ------------------------------");
		teller.setMinimumDownPercentage(bestParams.get(0));
		teller.setSellTriggerPercentage(bestParams.get(1));
		teller.setUpPercentageToBuy(bestParams.get(2));
		List<Double> profits = teller.doMagic(reader.readFile(filePath));
		System.out.println("Best strategy profit : " + profits.get(0));
		System.out.println("Holding profit : " + profits.get(1));
		*/
		
		
		// I want to compare for a certain range of 3 variables
		// How much percentage strategy can beat holding
		StrategyTeller teller = new StrategyTeller();
		Double bestStrategyProfit = Double.MIN_VALUE;
		List<Double> bestParams = Lists.newArrayList();
		
		int strategyWin = 0, holdingWin = 0;
		double strategyWinAmount = 0, holdingWinAmount = 0;
		
		for (double i = 0.79; i <= 0.85; i+=0.01) { // minimumDownPercentage
			for (double j = 0.58; j <= 0.63; j+=0.01) { // sellTriggerPercentage
				for (double k = 0.08; k <= 0.14; k+=0.01) { // upPercentageToBuy
					teller.setMinimumDownPercentage(i);
					teller.setSellTriggerPercentage(j);
					teller.setUpPercentageToBuy(k);
					
					List<Double> profits = teller.doMagic(reader.readFile(filePath));
					Double strategyProfit = profits.get(0);
					Double holdingProfit = profits.get(1);
					if (strategyProfit > holdingProfit) {
						strategyWin++;
						strategyWinAmount += strategyProfit-holdingProfit;
					} else if (strategyProfit < holdingProfit){
						holdingWin++;
						holdingWinAmount += holdingProfit-strategyProfit;
					}
				}
			}
		}
		
		System.out.println("Strategy wins " + strategyWin + " times with total margin as " + strategyWinAmount);
		System.out.println("Holding wins " + holdingWin + " times with total margin as " + holdingWinAmount);
	}
}
