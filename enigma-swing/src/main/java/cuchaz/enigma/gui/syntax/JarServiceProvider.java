/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cuchaz.enigma.gui.syntax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author subwiz
 * @author Ayman Al-Sairafi
 */
public class JarServiceProvider {

	public static final String SERVICES_ROOT = "META-INF/services/";
	private static final Logger LOG = Logger.getLogger(JarServiceProvider.class.getName());

	/**
	 * Prevents anyone from instantiating this class.
	 * Just use the static method
	 */
	private JarServiceProvider() {}

	private static ClassLoader getClassLoader() {
		ClassLoader cl = JarServiceProvider.class.getClassLoader();
		return cl == null ? ClassLoader.getSystemClassLoader() : cl;
	}

	/**
	 * Returns an Object array from the file in META-INF/resources/{classname}
	 *
	 * @throws java.io.IOException
	 */
	public static List<Object> getServiceProviders(Class cls) throws IOException {
		ArrayList<Object> l = new ArrayList<Object>();
		ClassLoader cl = getClassLoader();
		String serviceFile = SERVICES_ROOT + cls.getName();
		Enumeration<URL> e = cl.getResources(serviceFile);
		while (e.hasMoreElements()) {
			URL u = e.nextElement();
			InputStream is = u.openStream();
			BufferedReader br = null;
			try {
				br = new BufferedReader(
					new InputStreamReader(is, StandardCharsets.UTF_8));
				String str = null;
				while ((str = br.readLine()) != null) {
					int commentStartIdx = str.indexOf("#");
					if (commentStartIdx != -1) {
						str = str.substring(0, commentStartIdx);
					}
					str = str.trim();
					if (str.length() == 0) {
						continue;
					}
					try {
						Object obj = cl.loadClass(str).newInstance();
						l.add(obj);
					} catch (Exception ex) {
						LOG.warning("Could not load: " + str);
						LOG.warning(ex.getMessage());
					}
				}
			} finally {
				if (br != null) {
					br.close();
				}
			}
		}
		return l;
	}

	/**
	 * Reads a file in the META-INF/services location.  File name will be
	 * fully qualified classname, in all lower-case, appended with ".properties"
	 * If no file is found, then a an empty Property instance will be returned
	 *
	 * @return Property file read.
	 */
	public static Properties readProperties(Class clazz) {
		return readProperties(clazz.getName());
	}

	/**
	 * Reads a file in the META-INF/services named name appended with
	 * ".properties"
	 *
	 * If no file is found, then a an empty Property instance will be returned
	 *
	 * @param name name of file (use dots to separate subfolders).
	 * @return Property file read.
	 */
	public static Properties readProperties(String name) {
		Properties props = new Properties();
		String serviceFile = name.toLowerCase();
		if (!serviceFile.endsWith(".properties")) {
			serviceFile += ".properties";
		}
		InputStream is = findResource(serviceFile);
		if (is != null) {
			try {
				props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
			} catch (IOException ex) {
				Logger.getLogger(JarServiceProvider.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return props;
	}

	/**
	 * Reads language specific files in the META-INF/services named name appended
	 * with ".properties". The contents of the files are merged as follows:
	 * <ul>
	 *   <li>First the default language file (&lt;name&gt;.properties) is read</li>
	 *   <li>Then the general language variant of the file
	 *      (&lt;name&gt;_&lt;lang&gt;.properties) is read and its
	 *      entries are added to/overwrite the entries of the default life</li>
	 *   <li>Last the country specific language variant of the file
	 *      (&lt;name&gt;_&lt;lang&gt;_&lt;country&gt;.properties) is read and its
	 *      entries are added to/overwrite the existing entries</li>
	 * </ul>
	 * Example: You have three files:
	 * <ul>
	 *   <li>config.properties which contains the complete configuration
	 *     (most likely with English menus, tooltips)</li>
	 *   <li>config_de.properties which only contains menu names and tooltips
	 *     in German language</li>
	 *   <li>config_de_CH which might just contain entries for specific
	 *     Swiss spelling variant of some words in a tooltip</li>
	 * </ul>
	 *
	 * If no file is found, then a an empty Property instance will be returned
	 *
	 * @param name name of file (use dots to separate subfolders).
	 * @param locale The locale for which to read the files
	 * @return Property file read.
	 */
	public static Properties readProperties(String name, Locale locale) {
		// If name already ends in ".properties", then cut this off
		name = name.toLowerCase();
		int idx = name.lastIndexOf(".properties");
		if (idx > 0) {
			name = name.substring(0, idx);
		}
		// 1. Read properties of default langauge
		Properties props = readProperties(name);
		// 2. Read properties of general language variant
		if (locale != null && locale.getLanguage() != null) {
			name += "_"+locale.getLanguage();
			Properties langProps = readProperties(name);
			props.putAll(langProps);
		}
		// 3. Read properties of country specific language variant
		if (locale != null && locale.getCountry() != null) {
			name += "_"+locale.getCountry();
			Properties countryProps = readProperties(name);
			props.putAll(countryProps);
		}
		return props;
	}
	/**
	 * Read a file in the META-INF/services named name appended with
	 * ".properties", and returns it as a <code>Map&lt;String, String&gt;</code>
	 * If no file is found, then a an empty Property instance will be returned
	 * @param name name of file (use dots to separate subfolders).
	 * @return Map of keys and values
	 */
	public static Map<String, String> readStringsMap(String name) {
		Properties props = readProperties(name);
		HashMap<String, String> map = new HashMap<String, String>();
		if (props != null) {
			for (Map.Entry e : props.entrySet()) {
				map.put(e.getKey().toString(), e.getValue().toString());
			}
		}
		return map;
	}

	/**
	 * Read the given URL and returns a List of Strings for each input line
	 * Each line will not have the line terminator.
	 *
	 * The resource is searched in /META-INF/services/url, then in
	 * url, then the url is treated as a location in the current classpath
	 * and an attempt to read it from that location is done.
	 *
	 * @param url location of file to read
	 * @return List of Strings for each line read. or EMPTY_LIST if URL is not found
	 */
	@SuppressWarnings("unchecked")
	public static List<String> readLines(String url) {
		InputStream is = findResource(url);
		if (is == null) {
			return Collections.EMPTY_LIST;
		}
		List<String> lines = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				// Trim and unescape some control chars
				line = line.trim().replace("\\n", "\n").replace("\\t", "\t");
				lines.add(line);
			}
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, null, ex);
		} finally {
			try {
				is.close();
			} catch (IOException ex) {
				LOG.log(Level.SEVERE, null, ex);
			}
			return lines;
		}

	}

	/**
	 * Attempts to find a location url.  The following locations are searched in
	 * sequence:
	 * url,
	 * SERVICES_ROOT/url
	 * all classpath/url
	 *
	 * @return InputStream at that location, or null if not found
	 * @see JarServiceProvider#findResource(java.lang.String)
	 */
	public static InputStream findResource(String url, ClassLoader cl) {
		InputStream is = null;

		URL loc = cl.getResource(url);
		//  if (loc == null) {
		//    loc = cl.getResource(url);
		//  }
		if (loc == null) {
			loc = cl.getResource(SERVICES_ROOT + url);
		}
		if (loc == null) {
			is = ClassLoader.getSystemResourceAsStream(url);
		} else {
			try {
				is = loc.openStream();
			} catch (IOException ex) {
				Logger.getLogger(JarServiceProvider.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return is;
	}

	/**
	 * Attempts to find a location url.  The following locations are searched in
	 * sequence:
	 * url,
	 * SERVICES_ROOT/url
	 * all classpath/url
	 * The System ClassLoader is used.
	 *
	 * @return InputSTream at that location, or null if not found
	 * @see JarServiceProvider#findResource(java.lang.String, java.lang.ClassLoader)
	 */
	public static InputStream findResource(String url) {
		return findResource(url, getClassLoader());
	}
}
