# Weather-Studio

Weather Studio is a weather forecast app built in Kotlin using Retrofit, Gson library and
Dexter library.

## Link to the API:
<https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid=APIkey>

#### For more information :
<https://openweathermap.org/api>

## Retrofit 
A type-safe HTTP client for Android and Java.

#### For more information :
<https://square.github.io/retrofit/>

## Gson
Gson is a Java library that can be used to convert
Java Objects into their JSON representation. It can 
also be used to convert a JSON string to an equivalent
Java object. Gson can work with arbitrary Java objects
including pre-existing objects that you do not have 
source-code of.

## Download

##### Gradle

`
dependencies {
  implementation 'com.google.code.gson:gson:2.9.1'
}
`

##### Maven
`
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.9.1</version>
</dependency>
`

#### For more information :
<https://github.com/google/gson>

## Dexter
Android library that simplifies the process of requesting permissions at runtime.

##### Dependency

Include the library in your build.gradle
`
dependencies{
    implementation 'com.karumi:dexter:6.2.3'
}
`


To start using the library you just need to call Dexter with a valid Context:

`
public MyActivity extends Activity {
	@Override public void onCreate() {
		super.onCreate();
		Dexter.withContext(activity)
			.withPermission(permission)
			.withListener(listener)
			.check();
	}
}
`
#### For more information :
<https://github.com/Karumi/Dexter>

