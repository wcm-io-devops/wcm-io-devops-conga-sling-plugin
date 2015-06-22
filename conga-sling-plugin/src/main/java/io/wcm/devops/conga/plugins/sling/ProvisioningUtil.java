/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.devops.conga.plugins.sling;

import io.wcm.devops.conga.generator.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.io.ModelReader;

import com.google.common.collect.ImmutableList;

/**
 * Helper for handling provisioning file format.
 */
public final class ProvisioningUtil {

  private static final String FILE_EXTENSION = "txt";

  private ProvisioningUtil() {
    // static methods only
  }

  /**
   * Check if given file is a sling provisioning file.
   * @param file File
   * @param charset Charset
   * @return true if it seems to be so
   */
  public static boolean isProvisioningFile(File file, String charset) {
    try {
      return FileUtil.matchesExtension(file, FILE_EXTENSION)
          && StringUtils.contains(FileUtils.readFileToString(file, charset), "[feature ");
    }
    catch (IOException ex) {
      return false;
    }
  }

  /**
   * Parse provisioning file to model
   * @param file File
   * @param charset Charset
   * @return Model
   * @throws IOException
   */
  public static Model getModel(File file, String charset) throws IOException {
    try (InputStream is = new FileInputStream(file);
        Reader reader = new InputStreamReader(is, charset)) {
      Model model = ModelReader.read(reader, null);
      model = ModelUtility.getEffectiveModel(model, null);
      return model;
    }
  }

  /**
   * Visits OSGi configuration for all feature and run modes.
   * @param model Provisioning Model
   * @param consumer Configuration consumer
   * @throws IOException
   */
  public static void visitOsgiConfigurations(Model model, ConfigConsumer consumer) throws IOException {
    for (Feature feature : model.getFeatures()) {
      for (RunMode runMode : feature.getRunModes()) {
        for (Configuration configuration : runMode.getConfigurations()) {
          String path = getPathForConfiguration(configuration, runMode);
          consumer.accept(path, configuration.getProperties());
        }
      }
    }
  }

  /**
   * Get the relative path for a configuration
   */
  private static String getPathForConfiguration(Configuration configuration, RunMode runMode) {
    SortedSet<String> runModesList = new TreeSet<>();
    if (runMode.getNames() != null) {
      runModesList.addAll(ImmutableList.copyOf(runMode.getNames()));
    }

    // run modes directory
    StringBuilder path = new StringBuilder();
    if (!runModesList.isEmpty() && !runMode.isSpecial()) {
      path.append(StringUtils.join(runModesList, ".")).append("/");
    }

    // main name
    if (configuration.getFactoryPid() != null) {
      path.append(configuration.getFactoryPid()).append("-");
    }
    path.append(configuration.getPid()).append(".config");

    return path.toString();
  }

}
