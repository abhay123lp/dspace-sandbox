package org.dspace.maven.reports;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import org.apache.maven.doxia.sink.Sink;
import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * A maven 2.0 plugin for generating i18n reports on properties files.
 * 
 * @author mdiggory
 * @goal i18n-report
 * @phase site
 * @requiresDependencyResolution
 */
public class I18nReport extends AbstractMavenReport {

	/**
	 * Specifies the directory where the report will be generated
	 * 
	 * @parameter default-value="${project.reporting.outputDirectory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * <i>Maven Internal</i>: The Doxia Site Renderer.
	 * 
	 * @component
	 * @required
	 * @readonly
	 */
	private Renderer siteRenderer;

	/**
	 * <i>Maven Internal</i>: The Project descriptor.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	protected void executeReport(Locale arg0) throws MavenReportException {

		ClassLoader currentClassLoader = Thread.currentThread()
				.getContextClassLoader();

		try {
			
			ClassLoader contextClassloader = this.getClassLoader(currentClassLoader);

			ResourceBundle messages = ResourceBundle.getBundle("Messages",Locale.getDefault(), contextClassloader);

			Sink sink = getSink();

			Locale[] locales = getExistingLocales(contextClassloader, "Messages");

			for (int i = 0; i < locales.length; i++) {

				ResourceBundle localeRB = ResourceBundle.getBundle("Messages",locales[i], contextClassloader);

				if(!localeRB.getLocale().equals(messages.getLocale()))
				{
				
				sink.section1();
				sink.sectionTitle();
				sink.text(localeRB.getLocale().getDisplayName());
				sink.sectionTitle_();
				
				sink.table();

			
				sink.tableRow();

				sink.tableHeaderCell();
				sink.text("Key");
				sink.tableHeaderCell_();
				
				sink.tableHeaderCell();
				sink.text("English Value");
				sink.tableHeaderCell_();

				sink.tableHeaderCell();
				sink.text("Correction");
				sink.tableHeaderCell_();

				sink.tableRow_();
			

				for (Enumeration<String> e = messages.getKeys(); e
						.hasMoreElements();) {
					
					String key = e.nextElement();

					String value = messages.getString(key);
					
					String localValue = localeRB.getString(key);
					
					if(value.equals(localValue))
					{
						sink.tableRow();

						sink.tableCell();
						sink.text(key);
						sink.tableCell_();

						sink.tableCell();
						sink.text(localValue);
						sink.tableCell_();

						sink.tableCell();
						sink.rawText("<input type='text' name='" + localeRB.getLocale() + key + "'/>");
						
						sink.tableCell_();

						sink.tableRow_();
					}

					
				}
				
				sink.table_();
				
				sink.section1_();

				}
			}

			
		} catch (MojoExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// be sure to restore original context classloader
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	public static Locale[] getExistingLocales(ClassLoader loader,
			String baseName) {

		Locale[] locales = Locale.getAvailableLocales();

		TreeSet<Locale> existing = new TreeSet<Locale>(new Comparator() {

			public int compare(Object arg0, Object arg1) {

				if (arg0 instanceof Locale && arg1 instanceof Locale) {
					return ((Locale) arg0).getDisplayName().compareTo(
							((Locale) arg1).getDisplayName());
				}

				return arg0.toString().compareTo(arg1.toString());
			}

		});

		existing.add((Locale) new Locale(""));

		String bundleName = null;
		String localeSuffix = null;
		Locale l = null;

		for (int i = 0; i < locales.length; i++) {
			l = locales[i];
			bundleName = baseName;
			localeSuffix = l.toString();

			if (localeSuffix.length() > 0) {
				bundleName += "_" + localeSuffix;
			} else if (locales[i].getVariant().length() > 0) {
				bundleName += "___" + locales[i].getVariant();
			}

			if (verifyBundle(loader, bundleName)) {
				existing.add((Locale) locales[i]);
			}
		}

		return existing.toArray(new Locale[1]);

	}

	private static boolean verifyBundle(final ClassLoader loader,
			String bundleName) {
		// Search for class file using class loader
		try {
			Class bundleClass;
			if (loader != null) {
				bundleClass = loader.loadClass(bundleName);
			} else {
				bundleClass = Class.forName(bundleName);
			}
			if (ResourceBundle.class.isAssignableFrom(bundleClass)) {
				return true;
			}
		} catch (Exception e) {
		} catch (LinkageError e) {
		}

		// Next search for a Properties file.
		final String resName = bundleName.replace('.', '/') + ".properties";
		Object result = java.security.AccessController
				.doPrivileged(new java.security.PrivilegedAction() {
					public Object run() {
						if (loader != null) {
							return loader.getResource(resName);
						} else {
							return ClassLoader.getSystemResource(resName);
						}
					}
				});

		return result != null;
	}

	public String getOutputName() {
		return "i18n/index";
	}

	public String getName(Locale arg0) {
		return "I18N Properties report";
	}

	public String getDescription(Locale arg0) {
		return "Report on the missing I18N properties.";
	}

	/**
	 * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
	 */
	protected String getOutputDirectory() {
		return outputDirectory.getAbsolutePath();
	}

	/**
	 * @return Returns the siteRenderer.
	 */
	public Renderer getSiteRenderer() {
		return siteRenderer;
	}

	protected MavenProject getProject() {
		return project;
	}

	/**
	 * Set up a classloader for the execution of the main class.
	 * 
	 * @return the classloader
	 * @throws MojoExecutionException
	 */
	private ClassLoader getClassLoader(ClassLoader parent)
			throws MojoExecutionException {
		List classpathURLs = new ArrayList();
		this.addRelevantProjectDependenciesToClasspath(classpathURLs);
		return new URLClassLoader((URL[]) classpathURLs
				.toArray(new URL[classpathURLs.size()]), parent);
	}

	/**
	 * Add any relevant project dependencies to the classpath. Takes
	 * includeProjectDependencies into consideration.
	 * 
	 * @param path
	 *            classpath of {@link java.net.URL} objects
	 * @throws MojoExecutionException
	 */
	private void addRelevantProjectDependenciesToClasspath(List path)
			throws MojoExecutionException {
		if (true) {
			try {
				getLog().debug("Project Dependencies will be included.");

				URL mainClasses = new File(project.getBuild()
						.getOutputDirectory()).toURL();
				
				getLog().debug("Adding to classpath : " + mainClasses);
				path.add(mainClasses);

				Set dependencies = project.getArtifacts();

				// system scope dependencies are not returned by maven 2.0. See
				// MEXEC-17
				dependencies.addAll(getSystemScopeDependencies());

				Iterator iter = dependencies.iterator();
				while (iter.hasNext()) {
					Artifact classPathElement = (Artifact) iter.next();
					getLog().debug(
							"Adding project dependency artifact: "
									+ classPathElement.getArtifactId()
									+ " to classpath");
					path.add(classPathElement.getFile().toURL());
				}

			} catch (MalformedURLException e) {
				throw new MojoExecutionException(
						"Error during setting up classpath", e);
			}
		} else {
			getLog().debug("Project Dependencies will be excluded.");
		}

	}

	private Collection getSystemScopeDependencies()
			throws MojoExecutionException {
		List systemScopeArtifacts = new ArrayList();

		for (Iterator artifacts = getAllDependencies().iterator(); artifacts
				.hasNext();) {
			Artifact artifact = (Artifact) artifacts.next();

			if (artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
				systemScopeArtifacts.add(artifact);
			}
		}
		return systemScopeArtifacts;
	}

	/**
	 * @component
	 */
	private ArtifactFactory artifactFactory;

	// generic method to retrieve all the transitive dependencies
	private Collection getAllDependencies() throws MojoExecutionException {
		List artifacts = new ArrayList();

		for (Iterator dependencies = project.getDependencies().iterator(); dependencies
				.hasNext();) {
			Dependency dependency = (Dependency) dependencies.next();

			String groupId = dependency.getGroupId();
			String artifactId = dependency.getArtifactId();

			VersionRange versionRange;
			try {
				versionRange = VersionRange.createFromVersionSpec(dependency
						.getVersion());
			} catch (InvalidVersionSpecificationException e) {
				throw new MojoExecutionException("unable to parse version", e);
			}

			String type = dependency.getType();
			if (type == null) {
				type = "jar"; //$NON-NLS-1$
			}
			String classifier = dependency.getClassifier();
			boolean optional = dependency.isOptional();
			String scope = dependency.getScope();
			if (scope == null) {
				scope = Artifact.SCOPE_COMPILE;
			}

			Artifact art = this.artifactFactory.createDependencyArtifact(
					groupId, artifactId, versionRange, type, classifier, scope,
					optional);

			if (scope.equalsIgnoreCase(Artifact.SCOPE_SYSTEM)) {
				art.setFile(new File(dependency.getSystemPath()));
			}

			List exclusions = new ArrayList();
			for (Iterator j = dependency.getExclusions().iterator(); j
					.hasNext();) {
				Exclusion e = (Exclusion) j.next();
				exclusions.add(e.getGroupId() + ":" + e.getArtifactId()); //$NON-NLS-1$
			}

			ArtifactFilter newFilter = new ExcludesArtifactFilter(exclusions);

			art.setDependencyFilter(newFilter);

			artifacts.add(art);
		}

		return artifacts;
	}

}
