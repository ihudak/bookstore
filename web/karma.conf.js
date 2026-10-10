// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

const fs = require('fs');
const os = require('os');
const path = require('path');

// Without CHROME_BIN, fall back to the newest Chromium downloaded by
// `npx playwright install chromium` (the AI sandbox has no system Chrome).
// If none is found, karma-chrome-launcher looks for a system Chrome as usual.
function findPlaywrightChromium() {
  const cache = process.env.PLAYWRIGHT_BROWSERS_PATH || path.join(os.homedir(), '.cache', 'ms-playwright');
  let revisions;
  try {
    revisions = fs.readdirSync(cache)
      .map(dir => /^chromium-(\d+)$/.exec(dir))
      .filter(Boolean)
      .sort((a, b) => Number(b[1]) - Number(a[1]));
  } catch {
    return undefined;
  }
  for (const [dir] of revisions) {
    for (const build of ['chrome-linux64', 'chrome-linux']) {
      const bin = path.join(cache, dir, build, 'chrome');
      if (fs.existsSync(bin)) {
        return bin;
      }
    }
  }
  return undefined;
}

if (!process.env.CHROME_BIN) {
  const chromium = findPlaywrightChromium();
  if (chromium) {
    process.env.CHROME_BIN = chromium;
  }
}

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage')
    ],
    client: {
      jasmine: {
        // you can add configuration options for Jasmine here
        // the possible options are listed at https://jasmine.github.io/api/edge/Configuration.html
      }
    },
    jasmineHtmlReporter: {
      suppressAll: true // removes the duplicated traces
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/bookstore'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' }
      ]
    },
    reporters: ['progress', 'kjhtml'],
    // Chrome cannot use its own sandbox inside containers (AI sandbox, CI runners).
    browsers: ['ChromeHeadlessNoSandbox'],
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox']
      }
    },
    restartOnFileChange: true
  });
};
