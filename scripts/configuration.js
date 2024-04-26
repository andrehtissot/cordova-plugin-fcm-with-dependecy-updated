'use strict';

const helpers = require('./helpers');
const configPreferences = require('./get_config_preferences').getPreferences();

let APP_NAME = helpers.getValueFromXml(
    'config.xml',
    'name',
    'app name was not found on config.xml'
);

console.log("###### DETECTED PREFERENCES: " + JSON.stringify(configPreferences) + " ######")

const ENV = configPreferences.common.env;

console.log("###### USING ENV PROPERTY: " + ENV + " ######");

let IOS_DIR = 'platforms/ios';
let ANDROID_DIR = 'platforms/android';

exports.PLATFORM = {
    IOS: {
        label: 'ios',
        dir: IOS_DIR,
        googleServiceDestinations: [
            IOS_DIR + '/' + APP_NAME + '/Resources/GoogleService-Info.plist',
            IOS_DIR + '/' + APP_NAME + '/Resources/Resources/GoogleService-Info.plist'
        ],
        googleServiceSources: [
            'GoogleService-Info.plist',
            IOS_DIR + '/www/GoogleService-Info.plist',
            'www/GoogleService-Info.plist'
        ]
    },
    ANDROID: {
        label: 'android',
        dir: ANDROID_DIR,
        googleServiceDestinations: [
            ANDROID_DIR + '/google-services.json',
            ANDROID_DIR + '/app/google-services.json'
        ],
        googleServiceSources: [
            ANDROID_DIR + '/google-services.json',
            ANDROID_DIR + '/assets/www/google-services.json',
            'www/google-services.json',
            'google-services.json',
            ANDROID_DIR + '/' + ENV + '-google-services.json',
            ANDROID_DIR + '/assets/www/' + ENV + '-google-services.json',
            'www/' + ENV + '-google-services.json',
            ENV + '-google-services.json'
        ],
        stringsXmls: [
            ANDROID_DIR + '/app/build/generated/res/google-services/debug/values/values.xml'
        ]
    }
};
