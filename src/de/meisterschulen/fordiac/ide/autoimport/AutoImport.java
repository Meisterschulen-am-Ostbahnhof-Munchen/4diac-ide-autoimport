/*
 * Copyright (c) 2026 Meisterschulen am Ostbahnhof Muenchen
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package de.meisterschulen.fordiac.ide.autoimport;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;

/**
 * Imports every project folder directly below the workspace root that is not
 * yet part of the workspace. Runs once per IDE start, after the workbench is up.
 *
 * Rules: only direct sub folders containing a {@code .project} file, folders
 * starting with '.' (e.g. {@code .lib}, {@code .metadata}) are skipped, and the
 * project name in {@code .project} must equal the folder name.
 */
public class AutoImport implements IStartup {

	private static final String BUNDLE_ID = "de.meisterschulen.fordiac.ide.autoimport"; //$NON-NLS-1$

	@Override
	public void earlyStartup() {
		final IWorkspace ws = ResourcesPlugin.getWorkspace();
		final WorkspaceJob job = new WorkspaceJob("Import workspace projects") { //$NON-NLS-1$
			@Override
			public IStatus runInWorkspace(final IProgressMonitor monitor) {
				importProjects(ws, monitor);
				return Status.OK_STATUS;
			}
		};
		job.setRule(ws.getRoot());
		job.schedule();
	}

	private static void importProjects(final IWorkspace ws, final IProgressMonitor monitor) {
		final File root = ws.getRoot().getLocation().toFile();
		final File[] dirs = root.listFiles(File::isDirectory);
		if (dirs == null) {
			return;
		}
		for (final File dir : dirs) {
			final File dotProject = new File(dir, ".project"); //$NON-NLS-1$
			if (dir.getName().startsWith(".") || !dotProject.isFile()) { //$NON-NLS-1$
				continue;
			}
			try {
				final IProjectDescription desc = ws.loadProjectDescription(new Path(dotProject.getAbsolutePath()));
				final IProject project = ws.getRoot().getProject(desc.getName());
				if (project.exists()) {
					continue;
				}
				if (!desc.getName().equals(dir.getName())) {
					log(IStatus.WARNING, "Skipped: project name '" + desc.getName() //$NON-NLS-1$
							+ "' does not match folder '" + dir.getName() + "'", null); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
				desc.setLocation(null); // default location = folder in the workspace root
				project.create(desc, monitor);
				project.open(monitor);
				log(IStatus.INFO, "Imported: " + desc.getName(), null); //$NON-NLS-1$
			} catch (final CoreException e) {
				log(IStatus.ERROR, "Import failed: " + dir, e); //$NON-NLS-1$
			}
		}
	}

	private static void log(final int severity, final String msg, final Throwable t) {
		ILog.of(AutoImport.class).log(new Status(severity, BUNDLE_ID, msg, t));
	}
}
