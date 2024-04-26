'use strict';

const fs = require("fs"),
  et = require("elementtree");

exports.getPreferences = function () {

  const platformConfig = (function () {

    return {
      // Parses a given file into an elementtree object
      parseElementtreeSync: function (filename) {
        let contents = fs.readFileSync(filename, 'utf-8');
        if (contents) {
          //Windows uses the BOM. Skip the Byte Order Mark.
          contents = contents.substring(contents.indexOf('<'));
        }
        return new et.ElementTree(et.XML(contents));
      },
      /* Retrieves all <preferences ..> from config.xml and returns a map of preferences with platform as the key.
         If any platforms are supplied, common prefs + platform prefs will be returned, otherwise just common prefs are returned.
       */
      getPreferences: function (platforms) {
        const configXml = this.parseElementtreeSync("config.xml");
        let prefs = {
          common: {}
        };
        const commonPreferences = configXml.findall('preference');
        commonPreferences.forEach(function (el, i) {
          prefs.common[el.attrib.name] = el.attrib.value;
        })
        platforms.forEach(function (platform) {
          prefs[platform] = {};
          let platformPreferences = configXml.findall('platform[@name=\'' + platform + '\']/preference');
          platformPreferences.forEach(function (el, i) {
            prefs[platform][el.attrib.name] = el.attrib.value;
          });
        });
        return prefs;
      }
    };
  })();

  return platformConfig.getPreferences(['android', 'ios']);
}