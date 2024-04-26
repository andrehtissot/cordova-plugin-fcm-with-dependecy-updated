const fs = require('fs');
const path = require('path');

module.exports = function(context) {
    const gradleConfigPath = path.join(context.opts.projectRoot, 'platforms/android/cdv-gradle-config.json');

    fs.readFile(gradleConfigPath, 'utf8', (err, data) => {
        if (err) {
            console.error('❌ Error reading cdv-gradle-config.json:', err);
            throw err;
        }

        let config = JSON.parse(data);

        if (config.IS_GRADLE_PLUGIN_GOOGLE_SERVICES_ENABLED === false) {
            config.IS_GRADLE_PLUGIN_GOOGLE_SERVICES_ENABLED = true;

            fs.writeFile(gradleConfigPath, JSON.stringify(config, null, 2), (err) => {
                if (err) {
                    console.error('❌ Error updating cdv-gradle-config.json:', err);
                    throw err;
                } else {
                    console.log('✅ cdv-gradle-config.json updated. Google Services enabled.');
                }
            });
        } else {
            console.log('✅ Google Services is already set TRUE in cdv-gradle-config.json.');
        }
    });
};
