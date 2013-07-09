package com.suicune.dbwriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DBWriter {

	public Path fileName;
	public String packageName;

	/**
	 * Change the new DBWriter parameter to your package name.
	 */
	public static void main(String[] args) {
		new DBWriter("com.is.a.test").run();
	}

	public DBWriter(String packageName) {
		this.packageName = packageName + ".database";
	}

	public void run() {
		if (chooseFile()) {
			DB db = readFile(fileName);
			if (db != null) {
				writeContract(db);
				writeOpenHelper(db);
				writeProvider(db);
				outputScreen();
			}
		}
	}

	private class DB {
		public String mName;
		public ArrayList<Table> mTables;

	}

	private class Table {
		public String mName;
		public ArrayList<Field> mFields;
	}

	private class Field {
		public String mName;
		public String mType;
	}

	private boolean chooseFile() {
		try {
			fileName = Paths.get("/home/lapuente/test");

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private DB readFile(Path fileName) {
		DB db = null;
		try (BufferedReader reader = Files.newBufferedReader(fileName,
				Charset.defaultCharset())) {
			db = new DB();
			db.mTables = new ArrayList<Table>();

			db.mName = reader.readLine();

			String line = reader.readLine();
			// Process the tables
			while (line != null) {
				Table table = new Table();
				table.mFields = new ArrayList<Field>();
				table.mName = line;
				line = reader.readLine();
				// Process the fields of the table
				while (line != null && !line.equals("")) {
					Field field = fillField(line);
					table.mFields.add(field);
					line = reader.readLine();
				}

				db.mTables.add(table);
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return db;
	}

	private void writeContract(DB db) {
		String modifiers = "public static final String ";
		String className = db.mName + "Contract";
		Path path = Paths.get(fileName.getParent().toAbsolutePath() + "/"
				+ className + ".java");
		try (MyWriter writer = new MyWriter(Files.newBufferedWriter(path,
				Charset.defaultCharset()))) {
			writer.write("package " + packageName + ";");
			writer.write("");
			writer.write("public final class " + className + " {");
			writer.write("");
			for (Table table : db.mTables) {
				writer.write("public static class " + table.mName
						+ " implements BaseColumns {");
				writer.write(modifiers + "TABLE_NAME = \""
						+ table.mName.toLowerCase() + "\";");
				writer.write(modifiers + "DEFAULT_ORDER = _ID + \" DESC\";");
				writer.write("");
				for (Field field : table.mFields) {
					writer.write(modifiers + field.mName.toUpperCase()
							+ " = \"" + field.mName.toLowerCase() + "\";");

				}
				writer.write("}");
				writer.write("");
			}
			writer.write("}");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeOpenHelper(DB db) {
		String className = db.mName + "OpenHelper";
		String contract = db.mName + "Contract";
		Path path = Paths.get(fileName.getParent().toAbsolutePath() + "/"
				+ className + ".java");

		try (MyWriter writer = new MyWriter(Files.newBufferedWriter(path,
				Charset.defaultCharset()))) {
			writer.write("package " + packageName + ";");
			writer.write("");

			// Header and constants.
			writer.write("public class " + className
					+ " extends SQLiteOpenHelper {");
			writer.write("");
			writer.write("private static final String DB_NAME = \"" + db.mName
					+ "\";");
			writer.write("private static final int DB_VERSION = 1;");
			writer.write("");
			writer.write("private static final String CREATE = \"CREATE TABLE \";");
			writer.write("private static final String KEY = \" INTEGER PRIMARY KEY AUTOINCREMENT, \";");
			writer.write("");

			// Constructor
			writer.write("public " + className + "(Context context) {");
			writer.write("super(context, DB_NAME, null, DB_VERSION);");
			writer.write("}");
			writer.write("");

			// onCreate
			writer.write("@Override");
			writer.write("public void onCreate(SQLiteDatabase db) {");
			writer.write("if (db.isReadOnly()) {");
			writer.write("db = getWritableDatabase();");
			writer.write("}");
			writer.write("");

			// Tables
			for (Table table : db.mTables) {
				writer.write("db.execSQL(CREATE + " + contract + "."
						+ table.mName + ".TABLE_NAME + \" (\"");
				writer.write("+ " + contract + "." + table.mName + "._ID + KEY");
				for (int i = 0; i < table.mFields.size(); i++) {
					Field field = table.mFields.get(i);
					if (i != table.mFields.size() - 1) {
						writer.write("+ " + contract + "." + table.mName + "."
								+ field.mName.toUpperCase() + " + \""
								+ field.mType + ", \"");
					} else {
						writer.write("+ " + contract + "." + table.mName + "."
								+ field.mName.toUpperCase() + " + \""
								+ field.mType + ")\"");
					}
				}
				writer.write(");");
				writer.write("");
			}

			writer.write("}");

			writer.write("@Override");
			writer.write("public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {");
			writer.write("}");

			// End of file
			writer.write("}");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeProvider(DB db) {
		String className = db.mName + "Provider";
		String contract = db.mName + "Contract";
		Path path = Paths.get(fileName.getParent().toAbsolutePath() + "/"
				+ className + ".java");
		try (MyWriter writer = new MyWriter(Files.newBufferedWriter(path,
				Charset.defaultCharset()))) {
			writer.write("package " + packageName + ";");
			writer.write("");
			writer.write("public class " + className
					+ " extends ContentProvider {");

			// Content Name and content uris
			writer.write("protected static final String CONTENT_NAME = \""
					+ packageName + ".Provider\";");
			for (Table table : db.mTables) {
				writer.write("public static final Uri CONTENT_"
						+ table.mName.toUpperCase()
						+ " = Uri.parse(\"content://\" + CONTENT_NAME + \"/"
						+ table.mName.toLowerCase() + "\");");
			}
			writer.write("");

			writer.write(db.mName + "OpenHelper mDbHelper;");
			writer.write("");

			int id = 1;
			// ID Constants
			for (Table table : db.mTables) {
				writer.write("private static final int "
						+ table.mName.toUpperCase() + " = " + id++ + ";");
				writer.write("private static final int "
						+ table.mName.toUpperCase() + "_ID = " + id++ + ";");
			}
			writer.write("");

			// UriMatcher
			writer.write("static UriMatcher sUriMatcher;");
			writer.write("static {");
			writer.write("sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);");
			for (Table table : db.mTables) {
				writer.write("sUriMatcher.addURI(CONTENT_NAME, " + contract
						+ "." + table.mName + ".TABLE_NAME, "
						+ table.mName.toUpperCase() + ");");
				writer.write("sUriMatcher.addURI(CONTENT_NAME, " + contract
						+ "." + table.mName + ".TABLE_NAME + \"/#\", "
						+ table.mName.toUpperCase() + "_ID);");
			}
			writer.write("}");
			writer.write("");

			// OnCreate
			writer.write("@Override");
			writer.write("public boolean onCreate() {");
			writer.write("mDbHelper = new " + db.mName
					+ "OpenHelper(getContext());");
			writer.write("return mDbHelper != null;");
			writer.write("}");
			writer.write("");

			// getType
			writer.write("@Override");
			writer.write("public String getType(Uri uri) {");
			writer.write("switch(sUriMatcher.match(uri)) {");

			for (Table table : db.mTables) {
				writer.write("case " + table.mName.toUpperCase() + ":");
				writer.write("return ContentResolver.CURSOR_DIR_BASE_TYPE + CONTENT_NAME + \".\" + " + contract
						+ "." + table.mName + ".TABLE_NAME;");
				writer.write("case " + table.mName.toUpperCase() + "_ID:");
				writer.write("return ContentResolver.CURSOR_ITEM_BASE_TYPE + CONTENT_NAME + \".\" + " + contract
						+ "." + table.mName + ".TABLE_NAME;");
			}

			writer.write("default:");
			writer.write("return null;");
			writer.write("}");
			writer.write("}");
			writer.write("");

			// getTable
			writer.write("private String getTable(Uri uri) {");
			writer.write("switch(sUriMatcher.match(uri)) {");

			for (Table table : db.mTables) {
				writer.write("case " + table.mName.toUpperCase() + "_ID:");
				writer.write("case " + table.mName.toUpperCase() + ":");
				writer.write("return " + contract + "." + table.mName + ".TABLE_NAME;");
			}

			writer.write("default:");
			writer.write("return null;");
			writer.write("}");
			writer.write("}");
			writer.write("");

			// insert
			writer.write("@Override");
			writer.write("public Uri insert(Uri uri, ContentValues values) {");
			writer.write("String table = getTable(uri);");
			writer.write("long id = mDbHelper.getWritableDatabase().insert(table, null, values);");
			writer.write("Uri result = null;");
			writer.write("if (id != -1) {");
			writer.write("	result = ContentUris.withAppendedId(uri, id);");
			writer.write("}");
			writer.write("getContext().getContentResolver().notifyChange(uri, null);");
			writer.write("getContext().getContentResolver().notifyChange(result, null);");
			writer.write("return result;");
			writer.write("}");
			writer.write("");

			// query
			writer.write("@Override");
			writer.write("public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {");
			writer.write("String table = getTable(uri);");
			writer.write("boolean distinct = false;");
			writer.write("String groupBy = null;");
			writer.write("String having = null;");
			writer.write("String limit = null;");
			writer.write("Cursor cursor = mDbHelper.getReadableDatabase().query(distinct, table,");
			writer.write("		projection, selection, selectionArgs, groupBy, having,");
			writer.write("		sortOrder, limit);");
			writer.write("cursor.setNotificationUri(getContext().getContentResolver(), uri);");
			writer.write("return cursor;");
			writer.write("}");
			writer.write("");

			// delete
			writer.write("@Override");
			writer.write("public int delete(Uri uri, String selection, String[] selectionArgs) {");
			writer.write("String table = getTable(uri);");
			writer.write("int count = mDbHelper.getWritableDatabase().delete(table, selection,");
			writer.write("		selectionArgs);");
			writer.write("getContext().getContentResolver().notifyChange(uri, null);");
			writer.write("return count;");
			writer.write("}");
			writer.write("");

			// update
			writer.write("@Override");
			writer.write("public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {");
			writer.write("String table = getTable(uri);");
			writer.write("int count = mDbHelper.getWritableDatabase().update(table, values,");
			writer.write("		selection, selectionArgs);");
			writer.write("getContext().getContentResolver().notifyChange(uri, null);");
			writer.write("return count;");
			writer.write("}");
			writer.write("");

			// End of file
			writer.write("}");
			writer.write("");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Field fillField(String line) {
		Field field = new Field();
		field.mName = line.substring(0, line.indexOf(" "));
		field.mType = line.substring(line.indexOf(" ") + 1, line.length());
		return field;
	}

	public class MyWriter extends BufferedWriter {
		BufferedWriter writer;

		public MyWriter(BufferedWriter out) {
			super(out);
			writer = out;
		}

		@Override
		public void write(String str) throws IOException {
			writer.write(str);
			writer.newLine();
		}
	}
	
	private void outputScreen() {
		System.out.println("Please, add this lines to your manifest file, inside the \"Application\" tab:");
		System.out.println("<provider");
		System.out.println("android:name=\"" + packageName + ".Provider" + "\"");
		System.out.println("android:authorities=\"" + packageName + ".Provider" + "\"");
		System.out.println("android:exported=\"false\" >");
		System.out.println("</provider>");
	}
}