package exceptions;


/**
 * Exception to determine if the user has used a city name which doesn't exist in the OpenWeather API. Refer to https://openweathermap.org/api to get the valid city names.
 */
public class EmptyUserException extends Exception {
    private static final long serialVersionUID = 1L;

	public EmptyUserException() {
		super("Error when creating user, username was not found"); // Error message
	}
}
