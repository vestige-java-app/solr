package org.apache.solr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gaellalire
 */
public class SolrVestigeLauncher implements Callable<Void> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrVestigeLauncher.class);

	private File config;

	private File data;

	public SolrVestigeLauncher(final File config, final File data, File cache) {
		this.config = config;
		this.data = data;
		System.setProperty("jetty.base", data.getAbsolutePath());
		System.setProperty("jetty.port", "8983");
		System.setProperty("solr.solr.home", new File(data, "solr").getAbsolutePath());
	}

	public Void call() throws Exception {
		/*
		 * java.version.platform=8 java.version=1.8.0_241 java.version.micro=0
		 * jetty.home=/Users/gaellalire/Downloads/solr-8.6.1/server java.version.minor=8
		 * jetty.home.uri=file\:///Users/gaellalire/Downloads/solr-8.6.1/server
		 * jetty.base=/Users/gaellalire/Downloads/solr-8.6.1/server java.version.major=1
		 * jetty.base.uri=file\:///Users/gaellalire/Downloads/solr-8.6.1/server
		 */

		Thread launcherThread = new Thread() {
			@Override
			public void run() {
				File[] args = new File[] { new File(config, "jetty.xml"), new File(config, "jetty-http.xml") };
				List<Object> objects = new ArrayList<Object>(args.length);
				try {
					try {
		                Properties properties = new Properties();
		                properties.putAll(System.getProperties());
						XmlConfiguration last = null;
						for (File arg : args) {
							XmlConfiguration configuration = new XmlConfiguration(Resource.newResource(arg));
							if (last != null)
								configuration.getIdMap().putAll(last.getIdMap());
	                        if (properties.size() > 0)
	                        {
	                            Map<String, String> props = new HashMap<String, String>();
	                            for (Object key : properties.keySet())
	                            {
	                                props.put(key.toString(), String.valueOf(properties.get(key)));
	                            }
	                            configuration.getProperties().putAll(props);
	                        }

							Object obj = configuration.configure();
							if (obj != null && !objects.contains(obj))
								objects.add(obj);
							last = configuration;
						}

						// For all objects created by XmlConfigurations, start them if they are
						// lifecycles.
						for (Object obj : objects) {
							if (obj instanceof LifeCycle) {
								LifeCycle lc = (LifeCycle) obj;
								if (!lc.isRunning())
									lc.start();
							}
						}
						Object mutex = new Object();
						synchronized (mutex) {
							System.out.println("WAITING");
							mutex.wait();
						}
					} catch (InterruptedException e) {
						System.out.println("INTERR");
						for (Object obj : objects) {
							if (obj instanceof LifeCycle) {
								LifeCycle lc = (LifeCycle) obj;
								lc.stop();
							}
						}
						System.out.println("STOPPED");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		launcherThread.start();
		try {
			synchronized (this) {
				wait();
			}
		} catch (InterruptedException e) {
			launcherThread.interrupt();
			while (true) {
				try {
					launcherThread.join();
					break;
				} catch (InterruptedException e1) {
					LOGGER.trace("Ignore interrupt", e1);
				}
			}
			Thread currentThread = Thread.currentThread();
			ThreadGroup threadGroup = currentThread.getThreadGroup();
			int activeCount = threadGroup.activeCount();
			while (activeCount != 1) {
				Thread[] list = new Thread[activeCount];
				int enumerate = threadGroup.enumerate(list);
				for (int i = 0; i < enumerate; i++) {
					Thread t = list[i];
					if (t == currentThread) {
						continue;
					}
					t.interrupt();
				}
				for (int i = 0; i < enumerate; i++) {
					Thread t = list[i];
					if (t == currentThread) {
						continue;
					}
					try {
						t.join();
					} catch (InterruptedException e1) {
						LOGGER.trace("Interrupted", e1);
						break;
					}
				}
				activeCount = threadGroup.activeCount();
			}
		}
		return null;
	}

	/*
	 * Start - stop test
	 */
	public static void main(String[] args) throws InterruptedException {
		Thread t = new Thread(new ThreadGroup("a"), "b") {
			@Override
			public void run() {
				File file = new File("conf");
				File data = new File("data");
				try {
					new SolrVestigeLauncher(file, data, data).call();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
		Thread.sleep(10000);
		t.interrupt();
		t.join();
	}

}
