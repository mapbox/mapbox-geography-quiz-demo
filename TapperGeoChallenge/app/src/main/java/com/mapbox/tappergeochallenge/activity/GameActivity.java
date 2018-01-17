package com.mapbox.tappergeochallenge.activity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.tappergeochallenge.Endpoint;
import com.mapbox.tappergeochallenge.EndpointException;
import com.mapbox.tappergeochallenge.R;
import com.mapbox.tappergeochallenge.WikidataRetrofitService;
import com.mapbox.tappergeochallenge.model.City;
import com.mapbox.tappergeochallenge.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static com.mapbox.tappergeochallenge.StringConstants.ONE_PLAYER_GAME;
import static com.mapbox.tappergeochallenge.StringConstants.PLAYER_ONE_NAME;
import static com.mapbox.tappergeochallenge.StringConstants.PLAYER_TWO_NAME;
import static com.mapbox.tappergeochallenge.StringConstants.TWO_PLAYER_GAME;
import static com.mapbox.tappergeochallenge.StringConstants.TYPE_OF_GAME;


public class GameActivity extends AppCompatActivity {

  @BindView(R.id.mapview)
  MapView mapView;
  @BindView(R.id.check_answer_fab)
  FloatingActionButton checkAnswerFab;
  @BindView(R.id.location_to_guess_tv)
  TextView locationToGuess;
  @BindView(R.id.player_one_points)
  TextView playerOnePointsTextView;
  @BindView(R.id.player_two_points)
  TextView playerTwoPointsTextView;

  private static int CAMERA_BOUNDS_PADDING = 220;
  private static int EASE_CAMERA_SPEED_IN_MS = 1500;
  private static final String WIKIDATA_API_BASE_URL = "https://www.wikidata.org/w/api.php?";
  private String TAG = "GameActivity";
  private MapboxMap mapboxMap;
  private Icon playerOneIcon;
  private Icon playerTwoIcon;
  private Icon bullsEyeIcon;
  private Player playerOne;
  private Player playerTwo;
  private Location locationOfRandomCity;
  private float distanceBetween;
  public static List<Feature> listOfCities;
  public City randomCityToUse;
  private boolean isSinglePlayerGame;
  private boolean isTwoPlayerGame;
  private boolean playerOneHasGuessed;
  private boolean playerTwoHasGuessed;
  private Intent intent;
  private retrofit2.Call<Feature[]> request;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.content_game);

    // Bind views via a third-party library named Butterknife
    ButterKnife.bind(this);

//    getWikidataInfo();


    HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient client = new OkHttpClient.Builder()
      .addInterceptor(interceptor)
      .build();


    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl("https://query.wikidata.org/")
      .client(client)
      .addConverterFactory(ScalarsConverterFactory.create())
      .build();


    WikidataRetrofitService wikidataRetrofitService = retrofit.create(WikidataRetrofitService.class);

    Call<String> call = wikidataRetrofitService.getQuery("SELECT%20%3Fcity%20%3FcityLabel%20(SAMPLE(%3Flocation)%20AS%20%3Flocation)%20(MAX(%3Fpopulation)%20AS%20%3Fpopulation)%20(SAMPLE(%3Flayer)%20AS%20%3Flayer)%0AWHERE%0A%7B%0A%20%20%3Fcity%20wdt%3AP31%2Fwdt%3AP279*%20wd%3AQ515%3B%0A%20%20%20%20%20%20%20%20wdt%3AP625%20%3Flocation%3B%0A%20%20%20%20%20%20%20%20wdt%3AP1082%20%3Fpopulation.%0A%20%20FILTER(%3Fpopulation%20%3E%3D%20500000).%0A%20%20BIND(%0A%20%20%20%20IF(%3Fpopulation%20%3C%201000000%2C%20%22%3C1M%22%2C%0A%20%20%20%20IF(%3Fpopulation%20%3C%202000000%2C%20%221M-2M%22%2C%0A%20%20%20%20IF(%3Fpopulation%20%3C%205000000%2C%20%222M-5M%22%2C%0A%20%20%20%20IF(%3Fpopulation%20%3C%2010000000%2C%20%225M-10M%22%2C%0A%20%20%20%20IF(%3Fpopulation%20%3C%2020000000%2C%20%2210M-20M%22%2C%0A%20%20%20%20%22%3E20M%22)))))%0A%20%20%20%20AS%20%3Flayer).%0A%20%20SERVICE%20wikibase%3Alabel%20%7B%20bd%3AserviceParam%20wikibase%3Alanguage%20%22en%22.%20%7D%0A%7D%0AGROUP%20BY%20%3Fcity%20%3FcityLabel");

    call.enqueue(new Callback<String>() {
      @Override
      public void onResponse(Call<String> call, retrofit2.Response<String> response) {
        Log.d(TAG, "onResponse: response.body() = " + response.body());
      }

      @Override
      public void onFailure(Call<String> call, Throwable t) {
        Log.d(TAG, "onFailure: ");
      }
    });
    /*int statusCode;

    try {

      try {

        try {
          Log.d(TAG, "sendQueryGET: response.body() = " + response.body());

          statusCode = response.code();

          if (statusCode < 200 || statusCode >= 300) {
            throw new EndpointException(this, String.valueOf(response.code()));
          }

//          HttpEntity entity = response.getEntity();


          System.out.println("----------------------------------------");
          System.out.println(response.body());
          _response = response.body().string();

//          EntityUtils.consume(entity);


        } finally {
          response.close();
        }
      } finally {
//        client.close();
      }
    } catch (Exception exception) {
      Log.d(TAG, "sendQueryGET: exception = " + exception);
      exception.printStackTrace();
    }*/


   /* setOneOrTwoPlayerGame();
    playerOne = new Player();
    playerTwo = new Player();

    if (isSinglePlayerGame) {
      playerOnePointsTextView.setVisibility(View.GONE);
      playerTwoPointsTextView.setVisibility(View.GONE);
    } else if (isTwoPlayerGame) {
      setGameCardviewInfo();
    }

    initializeIcons();

    checkAnswerFab.hide();

    getAndDisplayLocationToGuess();
*/
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(final MapboxMap mapboxMap) {
        /*GameActivity.this.mapboxMap = mapboxMap;
        setUpMapClickListener();
        setUpInfoWindowClick();
        setUpMarkerClick();
        adjustLogoOpacity();
        adjustAttributionOpacity();*/
      }
    });
  }

  private void adjustLogoOpacity() {
    int MAPBOX_LOGO_OPACITY = 70;
    ImageView logo = mapView.findViewById(R.id.logoView);
    logo.setAlpha(MAPBOX_LOGO_OPACITY);
  }

  private void adjustAttributionOpacity() {
    int ATTRIBUTION_OPACITY = 70;
    ImageView attribution = mapView.findViewById(R.id.attributionView);
    attribution.setAlpha(ATTRIBUTION_OPACITY);
  }

  private void setGameCardviewInfo() {
    playerOne.setPlayerName(intent.getStringExtra(PLAYER_ONE_NAME));
    playerTwo.setPlayerName(intent.getStringExtra(PLAYER_TWO_NAME));
    displayPlayersPoints();
  }

  private void setUpMapClickListener() {
    mapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
      @Override
      public void onMapClick(@NonNull LatLng clickPoint) {
        if (isSinglePlayerGame && !playerOneHasGuessed && !playerTwoHasGuessed) {
          mapboxMap.clear();
          makeMarker(clickPoint, getString(R.string.single_player_game_marker_title),
            getString(R.string.click_here_to_confirm_selection),
            playerOneIcon);
        }
        if (isSinglePlayerGame && playerOneHasGuessed && !playerTwoHasGuessed) {
          Snackbar.make(findViewById(android.R.id.content),
            R.string.player_one_already_chose_snackbar_message,
            Snackbar.LENGTH_SHORT).show();
        }

        if (isTwoPlayerGame && !playerOneHasGuessed && !playerTwoHasGuessed) {
          mapboxMap.clear();
          makeMarker(clickPoint, getResources().getString(R.string.player_one_selection, playerOne.getPlayerName()), getString(R.string.click_here_to_confirm_selection),
            playerOneIcon);
        }

        if (isTwoPlayerGame && playerOneHasGuessed && !playerTwoHasGuessed) {
          makeMarker(clickPoint, getResources().getString(R.string.player_two_selection, playerTwo.getPlayerName()),
            getString(R.string.click_here_to_confirm_selection),
            playerTwoIcon);

          for (int x = 0; x < mapboxMap.getMarkers().size(); x++) {
            if (mapboxMap.getMarkers().get(x).getIcon().equals(playerTwoIcon) &&
              mapboxMap.getMarkers().get(x).getPosition().getLatitude() != clickPoint.getLatitude()) {
              mapboxMap.getMarkers().get(x).remove();
            }
          }
        }

        if (isTwoPlayerGame && playerOneHasGuessed && playerTwoHasGuessed) {
          Snackbar.make(findViewById(android.R.id.content),
            R.string.both_players_have_already_chose_snackbar_message,
            Snackbar.LENGTH_SHORT).show();
        }
      }
    });
  }

  private void setUpInfoWindowClick() {
    mapboxMap.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
      @Override
      public boolean onInfoWindowClick(@NonNull Marker marker) {
        Icon iconOfSelectedMarker = marker.getIcon();
        if (isSinglePlayerGame && !playerOneHasGuessed) {
          playerOneHasGuessed = true;
          playerOne.setSelectedLatitude(marker.getPosition().getLatitude());
          playerOne.setSelectedLongitude(marker.getPosition().getLongitude());
          addBullsEyeMarkerToMap(marker.getPosition(), randomCityToUse.getCityName(), bullsEyeIcon);
          checkAnswerFab.setImageResource(R.drawable.ic_done_white);
          checkAnswerFab.show();
          moveCameraToSelectedMarker(marker);
        }

        if (isTwoPlayerGame && !playerOneHasGuessed && iconOfSelectedMarker == playerOneIcon) {
          playerOneHasGuessed = true;
          playerOne.setSelectedLatitude(marker.getPosition().getLatitude());
          playerOne.setSelectedLongitude(marker.getPosition().getLongitude());
        }

        if (isTwoPlayerGame && playerOneHasGuessed && iconOfSelectedMarker == playerTwoIcon) {
          playerTwo.setSelectedLatitude(marker.getPosition().getLatitude());
          playerTwo.setSelectedLongitude(marker.getPosition().getLongitude());
          checkAnswerFab.setImageResource(R.drawable.ic_done_all_white);
          checkAnswerFab.show();
          addBullsEyeMarkerToMap(marker.getPosition(), randomCityToUse.getCityName(), bullsEyeIcon);
          moveCameraToSelectedMarker(marker);
          playerTwoHasGuessed = true;
        }


        return false;
      }
    });
  }

  private void moveCameraToSelectedMarker(Marker marker) {
    if (marker != null) {
      LatLngBounds latLngBounds = null;
      if (isSinglePlayerGame) {
        latLngBounds = new LatLngBounds.Builder()
          .include(marker.getPosition())
          .include(new LatLng(locationOfRandomCity.getLatitude(), locationOfRandomCity.getLongitude()))
          .build();
      }
      if (isTwoPlayerGame) {
        latLngBounds = new LatLngBounds.Builder()
          .include(marker.getPosition())
          .include(new LatLng(locationOfRandomCity.getLatitude(), locationOfRandomCity.getLongitude()))
          .include(new LatLng(playerOne.getSelectedLatitude(), playerOne.getSelectedLongitude()))
          .build();

      }
      mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, CAMERA_BOUNDS_PADDING), EASE_CAMERA_SPEED_IN_MS);
    }
  }

  private void setUpMarkerClick() {
    mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
      @Override
      public boolean onMarkerClick(@NonNull Marker marker) {
        if (marker.getIcon() == bullsEyeIcon) {
          CameraPosition position = new CameraPosition.Builder()
            .target(new LatLng(marker.getPosition().getLatitude(),
              marker.getPosition().getLongitude()))
            .zoom(5)
            .build();
          mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 2500);
        }
        return false;
      }
    });
  }

  private void makeMarker(LatLng markerPoint, String title, String snippet, Icon chosenIcon) {
    Marker marker = mapboxMap.addMarker(new MarkerOptions()
      .position(markerPoint)
      .title(title)
      .snippet(snippet)
      .icon(chosenIcon));
    mapboxMap.selectMarker(marker);
  }

  private void initializeIcons() {
    playerOneIcon = IconFactory.getInstance(this).fromResource(R.drawable.player_one_icon);
    playerTwoIcon = IconFactory.getInstance(this).fromResource(R.drawable.player_two_icon);
    bullsEyeIcon = IconFactory.getInstance(this).fromResource(R.drawable.bullseye_outline_filled);
  }

  private void setOneOrTwoPlayerGame() {
    intent = getIntent();
    String typeOfGame = intent.getStringExtra(TYPE_OF_GAME);
    if (typeOfGame.equals(ONE_PLAYER_GAME)) {
      isSinglePlayerGame = true;
      isTwoPlayerGame = false;
    }
    if (typeOfGame.equals(TWO_PLAYER_GAME)) {
      isSinglePlayerGame = false;
      isTwoPlayerGame = true;
    }
  }

  private void getAndDisplayLocationToGuess() {
    Random rand = new Random();
    int randomInt = rand.nextInt(listOfCities.size()) + 1;
    randomCityToUse = new City();

    Feature randomCityFromList = listOfCities.get(randomInt);

    randomCityToUse.setCityName(randomCityFromList.getProperties().get("city").getAsString());

    String randomCityCoordinates = randomCityFromList.getGeometry().getCoordinates().toString();

    String removeFromLongPointOne = ", altitude=NaN]";
    String longPointOne = randomCityCoordinates.replaceAll(removeFromLongPointOne, "");

    String removeFromLongPointTwo = "Position \\[";
    String longPointTwo = longPointOne.replaceAll(removeFromLongPointTwo, "");

    String removeFromLongPointThree = " latitude=";
    String longPointThree = longPointTwo.replaceAll(removeFromLongPointThree, "");

    String removeFromLongPointFour = " longitude=";
    String longPointFour = longPointThree.replaceAll(removeFromLongPointFour, "");

    String[] parts = longPointFour.split(",");
    String part0 = parts[0];
    String part1 = parts[1];

    String finalShort = "longitude=";
    String part0shortened = part0.replaceAll(finalShort, "");

    double finalLatPoint = Double.valueOf(part1);
    double finalLongPoint = Double.valueOf(part0shortened);

    locationOfRandomCity = new Location("");

    locationOfRandomCity.setLatitude(finalLatPoint);
    locationOfRandomCity.setLongitude(finalLongPoint);

    randomCityToUse.setCityLocation(locationOfRandomCity);

    locationToGuess.setText(getResources().getString(R.string.location_to_guess, randomCityToUse.getCityName()));
  }

  private float checkDistance(Player playerToCheck) {
    float[] results = new float[1];
    Location.distanceBetween(locationOfRandomCity.getLatitude(),
      locationOfRandomCity.getLongitude(), playerToCheck.getSelectedLatitude(),
      playerToCheck.getSelectedLongitude(), results);
    return distanceBetween = results[0] / 1000;
  }


  private void addBullsEyeMarkerToMap(LatLng location, String title, Icon chosenIcon) {
    mapboxMap.addMarker(new MarkerOptions()
      .position(location)
      .title(title)
      .icon(chosenIcon));
  }

  private void calculateAndDisplayPlayerOneDistance() {
    checkDistance(playerOne);
    Snackbar.make(findViewById(android.R.id.content),
      getResources().getString(R.string.player_guess_distance, distanceBetween),
      Snackbar.LENGTH_SHORT).show();
  }

  private void calculateAndGivePointToWinner() {
    if (checkDistance(playerOne) < checkDistance(playerTwo)) {
      Snackbar.make(findViewById(android.R.id.content),
        getResources().getString(R.string.winner_announcement, playerOne.getPlayerName()),
        Snackbar.LENGTH_SHORT).show();
      playerOne.setPoints(playerOne.getPoints() + 1);
      displayPlayersPoints();
      flashTextAsPointIsAdded(playerOnePointsTextView);
    } else {
      Snackbar.make(findViewById(android.R.id.content),
        getResources().getString(R.string.winner_announcement, playerTwo.getPlayerName()),
        Snackbar.LENGTH_SHORT).show();
      playerTwo.setPoints(playerTwo.getPoints() + 1);
      displayPlayersPoints();
      flashTextAsPointIsAdded(playerTwoPointsTextView);
    }
    getAndDisplayLocationToGuess();
  }

  private void flashTextAsPointIsAdded(TextView textView) {
    Animation anim = new AlphaAnimation(0.0f, 1.0f);
    anim.setDuration(50); // Manage the blinking speed here
    anim.setStartOffset(1);
    anim.setRepeatMode(Animation.REVERSE);
    anim.setRepeatCount(8);
    textView.startAnimation(anim);
  }

  private void displayPlayersPoints() {
    if (isSinglePlayerGame) {
      playerOnePointsTextView.setVisibility(View.GONE);
      playerTwoPointsTextView.setVisibility(View.GONE);
    } else {
      if (playerOne.getPoints() == 0 && playerTwo.getPoints() == 0) {
        if (playerOne.getPlayerName() == null && !playerTwo.getPlayerName().isEmpty()) {
          setPlayerTextViews(playerOnePointsTextView, R.string.player_one_points, getString(R.string.default_player_one_name), 0);
          setPlayerTextViews(playerTwoPointsTextView, R.string.player_two_points, playerTwo.getPlayerName(), 0);
        }
        if (!playerTwo.getPlayerName().isEmpty() && playerTwo.getPlayerName().isEmpty()) {
          setPlayerTextViews(playerOnePointsTextView, R.string.player_one_points, playerOne.getPlayerName(), 0);
          setPlayerTextViews(playerTwoPointsTextView, R.string.player_two_points, getString(R.string.default_player_two_name), 0);
        }
        if (!playerTwo.getPlayerName().isEmpty() && !playerTwo.getPlayerName().isEmpty()) {
          setPlayerTextViews(playerOnePointsTextView, R.string.player_one_points, playerOne.getPlayerName(), 0);
          setPlayerTextViews(playerTwoPointsTextView, R.string.player_two_points, playerTwo.getPlayerName(), 0);
        }
      } else {
        if (playerOne.getPlayerName().isEmpty() && !playerTwo.getPlayerName().isEmpty()) {
          setPlayerTextViews(playerOnePointsTextView, R.string.player_one_points, getString(R.string.default_player_one_name), playerOne.getPoints());
          setPlayerTextViews(playerTwoPointsTextView, R.string.player_two_points, playerTwo.getPlayerName(), playerTwo.getPoints());
        }
        if (!playerTwo.getPlayerName().isEmpty() && playerTwo.getPlayerName().isEmpty()) {
          setPlayerTextViews(playerOnePointsTextView, R.string.player_one_points, playerOne.getPlayerName(), playerOne.getPoints());
          setPlayerTextViews(playerTwoPointsTextView, R.string.player_two_points, getString(R.string.default_player_two_name), playerTwo.getPoints());
        }
        if (!playerTwo.getPlayerName().isEmpty() && !playerTwo.getPlayerName().isEmpty()) {
          setPlayerTextViews(playerOnePointsTextView, R.string.player_one_points, playerOne.getPlayerName(), playerOne.getPoints());
          setPlayerTextViews(playerTwoPointsTextView, R.string.player_two_points, playerTwo.getPlayerName(), playerTwo.getPoints());
        }
      }
    }
  }

  private void setPlayerTextViews(TextView view, int stringId, String playerName, int numOfPoints) {
    view.setText(getResources().getString(stringId, playerName, numOfPoints));
  }

  @SuppressWarnings("unchecked")
  public static void printResult(HashMap<String, HashMap> rs, int size) {

    for (String variable : (ArrayList<String>) rs.get("result").get("variables")) {
      System.out.print(String.format("%-" + size + "." + size + "s", variable) + " | ");
    }
    System.out.print("\n");
    for (HashMap<String, Object> value : (ArrayList<HashMap<String, Object>>) rs.get("result").get("rows")) {
      //System.out.print(value);
        /* for (String key : value.keySet()) {
         System.out.println(value.get(key));
         }*/
      for (String variable : (ArrayList<String>) rs.get("result").get("variables")) {
        //System.out.println(value.get(variable));
        System.out.print(String.format("%-" + size + "." + size + "s", value.get(variable)) + " | ");
      }
      System.out.print("\n");
    }
  }

  private void getWikidataInfo() {
    try {
      Endpoint sp = new Endpoint("https://query.wikidata.org/sparql", false);

      String querySelect = "#Big cities, grouped into map layers by population\n" +
        "#defaultView:Map\n" +
        "SELECT DISTINCT ?city ?cityLabel (SAMPLE(?location) AS ?location) (MAX(?population) AS ?population) (SAMPLE(?layer) AS ?layer)\n" +
        "WHERE\n" +
        "{\n" +
        "  ?city wdt:P31/wdt:P279* wd:Q515;\n" +
        "        wdt:P625 ?location;\n" +
        "        wdt:P1082 ?population.\n" +
        "  FILTER(?population >= 500000).\n" +
        "  BIND(\n" +
        "    IF(?population < 1000000, \"<1M\",\n" +
        "    IF(?population < 2000000, \"1M-2M\",\n" +
        "    IF(?population < 5000000, \"2M-5M\",\n" +
        "    IF(?population < 10000000, \"5M-10M\",\n" +
        "    IF(?population < 20000000, \"10M-20M\",\n" +
        "    \">20M\")))))\n" +
        "    AS ?layer).\n" +
        "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }\n" +
        "}\n" +
        "GROUP BY ?city ?cityLabel";

      HashMap rs = sp.query(querySelect);
      printResult(rs, 30);
    } catch (EndpointException endpointException) {
      Log.d(TAG, "getWikidataInfo: eex = " + endpointException);
    }
  }

  @OnClick(R.id.check_answer_fab)
  public void checkAnswer(View view) {
    if (isSinglePlayerGame) {
      calculateAndDisplayPlayerOneDistance();
      playerOneHasGuessed = false;
      getAndDisplayLocationToGuess();
    }
    if (isTwoPlayerGame) {
      calculateAndGivePointToWinner();
      playerOneHasGuessed = false;
      playerTwoHasGuessed = false;
    }
    mapboxMap.clear();
    checkAnswerFab.hide();
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}
