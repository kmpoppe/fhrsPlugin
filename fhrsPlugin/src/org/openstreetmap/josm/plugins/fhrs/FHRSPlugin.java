package org.openstreetmap.josm.plugins.fhrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.actions.*;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.*;

import org.openstreetmap.josm.gui.*;

import org.openstreetmap.josm.plugins.*;

import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;

public class FHRSPlugin extends Plugin {
	static JMenu FHRSMenu;
	static JMenuItem FHRSGet;
	static JMenuItem FHRSSearch;
	private OsmPrimitive selectedObject;

	FHRSPlugin me = this;
	int maxMenuItemLen = 50;
	public static String crLf = "" + (char) 13 + (char) 10;
	public static String cApiEst = "https://api.ratings.food.gov.uk/Establishments";
	
	public FHRSPlugin(final PluginInformation info) {
		super(info);
		createMenu();
	}

	private void createMenu() {
		final MainMenu menu = MainApplication.getMenu();
		if (FHRSMenu == null) {
			FHRSMenu = menu.addMenu("FHRS", "FHRS", 0, menu.getDefaultMenuPos(), "help");
			FHRSGet = new JMenuItem("Get information");
			FHRSGet.setEnabled(true);
			FHRSGet.addActionListener(getFHRSAction);
			FHRSSearch = new JMenuItem("Search entry");
			FHRSSearch.setEnabled(true);
			FHRSSearch.addActionListener(searchFHRSAction);
			FHRSMenu.add(FHRSGet);
			FHRSMenu.add(FHRSSearch);
		}
	}

	private final JosmAction getFHRSAction = new JosmAction() {
		private static final long serialVersionUID = -1652587391210733657L;

		@Override
		public void actionPerformed(ActionEvent event) {
			DataSet currentDataSet = MainApplication.getLayerManager().getActiveDataSet();
			if (currentDataSet.getAllSelected().size() > 0) {
				if (currentDataSet.getAllSelected().size() > 1) {
					msgBox("More than one object selected", JOptionPane.WARNING_MESSAGE);
				} else {
					selectedObject = currentDataSet.getAllSelected().iterator().next();
					if (selectedObject.getKeys().containsKey("fhrs:id")) {
						updateObjectData(selectedObject.get("fhrs:id").toString());
					} else {
						msgBox("Object doesn't have an FHRS ID", JOptionPane.WARNING_MESSAGE);
					}
				}
			} else {
				msgBox("No object selected", JOptionPane.WARNING_MESSAGE);
			}
		}
	};

	private final JosmAction searchFHRSAction = new JosmAction() {
		private static final long serialVersionUID = 1073365716525873912L;

		@Override
		public void actionPerformed(ActionEvent event) {
			DataSet currentDataSet = MainApplication.getLayerManager().getActiveDataSet();
			if (currentDataSet.getAllSelected().size() > 0) {
				if (currentDataSet.getAllSelected().size() > 1) {
					msgBox("More than one object selected", JOptionPane.WARNING_MESSAGE);
				} else {
					selectedObject = currentDataSet.getAllSelected().iterator().next();
					String thisName = "", thisAddress = "";
					if (selectedObject.getKeys().containsKey("name")) {
						thisName = selectedObject.get("name");
					}
					String[] addrTags = new String[] { "housenumber", "street", "city", "postcode"};
					boolean moreThanHousenumber = false;
					for(String addrTag : addrTags) {
						if (selectedObject.getKeys().containsKey("addr:" + addrTag)) {
							thisAddress += (thisAddress != "" ? " " : "") + selectedObject.get("addr:" + addrTag);
							if (addrTag != "housenumber") moreThanHousenumber = true;
						}	
					}					
					if (moreThanHousenumber) {
						String returnedJson = "{}";
						String cEncodedName = "";
						String cEncodedAddress = "";
						try {
							cEncodedName = URLEncoder.encode(thisName, StandardCharsets.UTF_8.toString());
							cEncodedAddress = URLEncoder.encode(thisAddress, StandardCharsets.UTF_8.toString());
						} catch (Exception e) {
							// This shouldn't fail
						}
						try {
							Gson gson = new Gson();
							String cUrl = "";
							JsonArray jEstablishmentsArray;
							for (String[] pars: new String[][] { 
								{ "&name=" + cEncodedName, "&address=" + cEncodedAddress },
								{ "&address=" + cEncodedAddress },
								{ "&name=" + cEncodedName }
							} )
							{
								cUrl = cApiEst + "?pagesize=10";
								for(String par: pars) cUrl = cUrl + par;
								returnedJson = fhrsApiCall(cUrl);
								if (gson.fromJson(returnedJson, JsonObject.class).getAsJsonArray("establishments").size() > 0) break;
							}
							jEstablishmentsArray = 
								gson
								.fromJson(returnedJson, JsonObject.class)
								.getAsJsonArray("establishments");
							List<List<String>> searchResults = new ArrayList<List<String>>();
							for(JsonElement jEstablishmentElement: jEstablishmentsArray) {
								JsonObject jEstablishmentObject = jEstablishmentElement.getAsJsonObject();
								List<String> tableEntry = new ArrayList<String>();
								String ApiFHRSID = jEstablishmentObject.get("FHRSID").toString();
								String ApiBusinessName = jEstablishmentObject.get("BusinessName").toString().replaceAll("\"([^\"]*)\"", "$1");
								tableEntry.add(ApiFHRSID);
								tableEntry.add(ApiBusinessName);
								String ApiFullAddress = "";
								for (int iAddrLine = 1; iAddrLine < 5; iAddrLine++) {
									String ApiAddressEntry = jEstablishmentObject
										.get("AddressLine" + Integer.toString(iAddrLine))
										.toString()
										.replaceAll("\"([^\"]*)\"", "$1");
									ApiFullAddress += " " + ApiAddressEntry.trim();
									ApiFullAddress = ApiFullAddress.trim();
								}
								tableEntry.add(ApiFullAddress);
								searchResults.add(tableEntry);
							}
							String selectedFhrsId = searchResultsDialog.showSearchDialog(searchResults);
							if (selectedFhrsId != null) {
								updateObjectData(selectedFhrsId);
							}
						} catch (FileNotFoundException e) {
							msgBox("FHRS ID " + selectedObject.get("fhrs:id").toString() + " not found in database.", JOptionPane.ERROR_MESSAGE);
						} catch (Exception e) {
							String cStackTrace = "";
							for(StackTraceElement s: e.getStackTrace())
								cStackTrace += s.toString() + crLf;
							msgBox(e.toString() + cStackTrace, JOptionPane.ERROR_MESSAGE);
						}
					} else {
						msgBox("This object doesn't have an address. The FHRS API will not return data.", JOptionPane.ERROR_MESSAGE);	
					}
				}
			} else {
				msgBox("No object selected", JOptionPane.WARNING_MESSAGE);
			}
		}
	};

	public void updateObjectData(String fhrsId) {
		DataSet currentDataSet = MainApplication.getLayerManager().getActiveDataSet();
		if (currentDataSet.getAllSelected().size() > 0) {
			if (currentDataSet.getAllSelected().size() > 1) {
				msgBox("More than one object selected", JOptionPane.WARNING_MESSAGE);
			} else {
				selectedObject = currentDataSet.getAllSelected().iterator().next();
				if (fhrsId != "") {
					String returnedJson = "{}";
					try {
						returnedJson = fhrsApiCall(cApiEst + "/" + fhrsId);
					} catch (FileNotFoundException e) {
						msgBox("FHRS ID " + selectedObject.get("fhrs:id").toString() + " not found in database.", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						String cStackTrace = "";
						for(StackTraceElement s: e.getStackTrace())
							cStackTrace += s.toString() + crLf;
						msgBox(e.toString() + cStackTrace, JOptionPane.ERROR_MESSAGE);
					}
					Gson gson = new Gson();
					try {
						JsonObject jEstablishmentProperties = 
							gson
							.fromJson(returnedJson, JsonObject.class)
							.getAsJsonObject();
						Map<String, oldAndNewValues> osmTags = new HashMap<String, oldAndNewValues>();
						
						for(Map.Entry<String, String> entry : JsonToOSM.entrySet()) {
							String thisNewValue = jEstablishmentProperties
								.get(entry.getKey())
								.toString()
								.replaceAll("\"([^\"]*)\"", "$1");
							if (thisNewValue != "") {
								osmTags.put(
									entry.getValue(), 
									new oldAndNewValues(
										thisNewValue, 
										(selectedObject.get(entry.getValue()) != null ? selectedObject.get(entry.getValue()) : "")
									)
								);
							}
						}
						Map<String, String> osmTagsToSet = mergeTagsDialog.showTagsDialog(osmTags);
						if (osmTagsToSet != null && osmTagsToSet.size() > 0) {
							ChangePropertyCommand changePropertyCommand = 
								new ChangePropertyCommand(
									currentDataSet, 
									java.util.Collections.singleton(selectedObject), 
									osmTagsToSet
								);
							UndoRedoHandler.getInstance().add(changePropertyCommand);
						}
					} catch (Exception e) {
						String cStackTrace = "";
						for(StackTraceElement s: e.getStackTrace())
							cStackTrace += s.toString() + crLf;
						msgBox(e.toString() + cStackTrace, JOptionPane.ERROR_MESSAGE);
					}
				} else {
					msgBox("FHRS ID " + selectedObject.get("fhrs:id").toString() + " not found in database.", JOptionPane.ERROR_MESSAGE);
				}
			}
		} else {
			msgBox("No object selected", JOptionPane.WARNING_MESSAGE);
		}
	}

	public static String fhrsApiCall(String cUrl) throws FileNotFoundException, Exception {
		String returnValue = "";
		try {
			URL url = new URL(cUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("X-API-Version", "2");
			int status = con.getResponseCode();
 
			Reader streamReader = null;
			
			if (status > 299) {
				streamReader = new InputStreamReader(con.getErrorStream(), Charset.defaultCharset());
			} else {
				streamReader = new InputStreamReader(con.getInputStream(), Charset.defaultCharset());
			}
			
			BufferedReader in = new BufferedReader(streamReader);
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			returnValue = content.toString();
			in.close();
			con.disconnect();
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException(e.getMessage());
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return returnValue;
	}

	static final Map<String, String> JsonToOSM = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("BusinessName",				"name");
			put("AddressLine1",				"addr:unit");
			put("AddressLine2",				"addr:housenumber");
			put("AddressLine3",				"addr:street");
			put("AddressLine4",				"addr:city");
			put("PostCode",					"addr:postcode");
			put("FHRSID",					"fhrs:id");
			put("LocalAuthorityBusinessID",	"fhrs:local_authority_id");
			put("LocalAuthorityName", 		"fhrs:authority");
		}
	};

	public static void msgBox(String message, int messageType) {
		JOptionPane.showMessageDialog(null, message, "FHRS Plugin", messageType);
	}

	public static class oldAndNewValues {
		public String newValue;
		public String oldValue;
		public oldAndNewValues(String newV, String oldV) {
			this.newValue = newV;
			this.oldValue = oldV;
		}
	}
}
