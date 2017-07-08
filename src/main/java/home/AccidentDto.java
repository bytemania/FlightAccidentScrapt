package home;
import java.util.Date;

public class AccidentDto {
	private final Date date;
	private final String country;
	private final String location;
	private final String type;
	private final String model;
	private final String operator;
	private final String registration;
	private final String accident;
	private final String damage;
	private final Integer fatalities;

	public AccidentDto(Date date, String country, String location, String type, String model, String operator,
			String registration, String accident, String damage, Integer fatalities) {
		this.date = date;
		this.country = country;
		this.location = location;
		this.type = type;
		this.model = model;
		this.operator = operator;
		this.registration = registration;
		this.accident = accident;
		this.damage = damage;
		this.fatalities = fatalities;
	}

	public Date getDate() {
		return date;
	}

	public String getCountry() {
		return country;
	}

	public String getLocation() {
		return location;
	}

	public String getType() {
		return type;
	}

	public String getModel() {
		return model;
	}

	public String getOperator() {
		return operator;
	}

	public String getRegistration() {
		return registration;
	}

	public String getAccident() {
		return accident;
	}

	public String getDamage() {
		return damage;
	}

	public Integer getFatalities() {
		return fatalities;
	}
}
