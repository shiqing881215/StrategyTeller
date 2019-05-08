
public class DateAndPrice {

	private String date;
	private Double price;
	
	public DateAndPrice(String date, Double price) {
		super();
		this.date = date;
		this.price = price;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}
}
