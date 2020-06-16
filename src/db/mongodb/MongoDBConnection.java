package db.mongodb;

//This line needs manual import.
import static com.mongodb.client.model.Filters.eq;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;

public class MongoDBConnection implements DBConnection {

	  private static MongoDBConnection instance;

	  public static DBConnection getInstance() {
	    if (instance == null) {
	      instance = new MongoDBConnection();
	    }
	    return instance;
	  }

	  private MongoClient mongoClient;
	  private MongoDatabase db;

	  private MongoDBConnection() {
	    // Connects to local mongodb server.
	    mongoClient = new MongoClient();
	    db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
	  }

	  @Override
	  public void close() {
	    if (mongoClient != null) {
	      mongoClient.close();
	    }
	  }

	  @Override
	  public void setFavoriteItems(String userId, Item item) {
	    db.getCollection("users").updateOne(new Document("user_id", userId),
	        new Document("$push", new Document("favorite", item.getItemId())));
	  }

	  @Override
	  public void unsetFavoriteItems(String userId, String itemId) {
	    db.getCollection("users").updateOne(new Document("user_id", userId),
	        new Document("$pull", new Document("favorite", itemId)));
	  }

	  @Override
	  public Set<String> getFavoriteItemIds(String userId) {
	    Set<String> favoriteItems = new HashSet<String>();
	    // db.users.find({user_id:1111})
	    FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
	    if (iterable.first().containsKey("favorite")) {
	      @SuppressWarnings("unchecked")
	      List<String> list = (List<String>) iterable.first().get("favorite");
	      favoriteItems.addAll(list);
	    }
	    return favoriteItems;
	  }

	  @Override
	  public Set<Item> getFavoriteItems(String userId) {
	    Set<String> itemIds = getFavoriteItemIds(userId);
	    Set<Item> favoriteItems = new HashSet<>();
	    for (String itemId : itemIds) {
	      FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
	      Document doc = iterable.first();
	      ItemBuilder builder = new ItemBuilder();
	      builder.setItemId(doc.getString("item_id"));
	      builder.setName(doc.getString("name"));
	      builder.setCity(doc.getString("city"));
	      builder.setState(doc.getString("state"));
	      builder.setCountry(doc.getString("country"));
	      builder.setZipcode(doc.getString("zipcode"));
	      builder.setRating(doc.getDouble("rating"));
	      builder.setAddress(doc.getString("address"));
	      builder.setLatitude(doc.getDouble("latitude"));
	      builder.setLongitude(doc.getDouble("longitude"));
	      builder.setDescription(doc.getString("description"));
	      builder.setImageUrl(doc.getString("image_url"));
	      builder.setUrl(doc.getString("url"));
	      // Get categories information
	      iterable = db.getCollection("categories").find(eq("item_id", itemId));
	      Set<String> categories = new HashSet<>();
	      // Anonymous class.
	      iterable.forEach(new Block<Document>() {
	        @Override
	        public void apply(Document doc) {
	          categories.add(doc.getString("category"));
	        }
	      });
	      builder.setCategories(categories);

	      favoriteItems.add(builder.build());
	    }
	    return favoriteItems;
	  }

	  @Override
	  public Set<String> getCategories(String itemId) {
	    Set<String> categories = new HashSet<>();
	    FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));

	    if (iterable.first().containsKey("categories")) {
	      @SuppressWarnings("unchecked")
	      List<String> list = (List<String>) iterable.first().get("categories");
	      categories.addAll(list);
	    }
	    return categories;
	  }

	  @Override
	  public void saveItem(Item item) {
	    UpdateOptions options = new UpdateOptions().upsert(true);
	    db.getCollection("items").updateOne(new Document().append("item_id", item.getItemId()),
	        new Document("$set",
	            new Document().append("item_id", item.getItemId()).append("name", item.getName())
	                .append("city", item.getCity()).append("state", item.getState())
	                .append("country", item.getCountry()).append("zip_code", item.getZipcode())
	                .append("rating", item.getRating()).append("address", item.getAddress())
	                .append("latitude", item.getLatitude()).append("longitude", item.getLongitude())
	                .append("description", item.getDescription())
	                .append("image_url", item.getImageUrl()).append("url", item.getUrl())
	                .append("categories", item.getCategories())),
	        options);
	  }

	  @Override
	  public String getFullname(String userId) {
	    FindIterable<Document> iterable =
	        db.getCollection("users").find(new Document("user_id", userId));
	    Document document = iterable.first();
	    String firstName = document.getString("first_name");
	    String lastName = document.getString("last_name");
	    return firstName + " " + lastName;
	  }

	  @Override
	  public boolean verifyLogin(String userId, String password) {
	    FindIterable<Document> iterable =
	        db.getCollection("users").find(new Document("user_id", userId));
	    Document document = iterable.first();
	    return document.getString("password").equals(password);
	  }

	  @Override
	  public boolean registerUser(String userId, String password, String firstname, String lastname) {
	    FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));

	    if (iterable.first() == null) {
	      db.getCollection("users").insertOne(new Document().append("first_name", firstname)
	          .append("last_name", lastname).append("password", password).append("user_id", userId));
	      return true;
	    }
	    return false;
	  }
	}
