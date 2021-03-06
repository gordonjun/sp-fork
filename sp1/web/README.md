# The new SP GUI

**Generated from HotTowel Angular**

>*Opinionated Angular style guide for teams by [@john_papa](//twitter.com/john_papa)*

>More details about the styles and patterns used in this app can be found in my [Angular Style Guide](https://github.com/johnpapa/angularjs-styleguide) and the [Angular Patterns: Clean Code](http://jpapa.me/ngclean) course at [Pluralsight](http://pluralsight.com/training/Authors/Details/john-papa) and working in teams.

## Prerequisites

1. Install [Node.js](http://nodejs.org)
 - on OSX use [homebrew](http://brew.sh) `brew install node`
 - on Windows use [chocolatey](https://chocolatey.org/) `choco install nodejs`
 
2. Install Gulp and Bower globally.    
    ```bash
    npm install -g gulp bower
    ```
    
    >Refer to these [instructions on how to not require sudo](https://github.com/sindresorhus/guides/blob/master/npm-global-without-sudo.md)

3. Install the rest of the app dependencies.    
    ```bash
    npm install
    bower install
    ```

## Running the GUI project
### Running in dev mode
 - Run the project with `gulp serve-dev`.
 - Opens the app in a browser and refreshes the browser on any file change.
 
### Unit Tests
 - Run the unit tests using `gulp unit-test` (via Karma, Mocha and Sinon).

#### Note on PhantomJS function undefined errors
Since the inclusion of the ng-jsoneditor library, Karma tests fail with a "function undefined" error on PhantomJS 1.0. 
This is because PhantomJS 1.0 does not support some vital ES5 and ES6 specifications, like bind(). 
PhantomJS 2.0, released in january 2015, fixed this, but still working binaries and npm packages are missing for 
many platforms. Therefore, PhantomJS 2.0 is not available through npm. Luckily, the PhantomJS 1.0 binary is easy to 
replace with its 2.0 counterpart. Windows and Mac OSX binaries could be downloaded from 
http://phantomjs.org/download.html while  brprodoehl provides binaries for Ubuntu at 
https://github.com/bprodoehl/phantomjs/releases. Replace the existing PhantomJS 1.0 binary in 
node_modules/phantomjs/lib/phantom/bin with one of these and then hopefully you are good to go.

### E2E Tests
 - Run the E2E tests using `gulp e2e-test` (via Protractor, Selenium WebDriver and Jasmine).

### Linting
 - Run code analysis using `gulp vet`. This runs jshint, jscs, and plato.

### Building the project
 - Build the optimized project using `gulp build`.
 - This create the optimized code for the project and puts it in the build folder.

### Running the optimized code
 - Run the optimize project from the build folder with `gulp serve-build`.

## Exploring HotTowel
HotTowel Angular starter project.

### Structure
The structure also contains a gulpfile.js.

	/src
		/app
		/content

### The Modules

#### core Module
Core modules are ones that are shared throughout the entire application and may be customized for the specific application. Example might be common data services.

This is an aggregator of modules that the application will need. The `core` module takes the blocks, common, and Angular sub-modules as dependencies.

#### blocks Modules
Block modules are reusable blocks of code that can be used across projects simply by including them as dependencies.

##### blocks.logger Module
The `blocks.logger` module handles logging across the Angular app.

##### blocks.exception Module
The `blocks.exception` module handles exceptions across the Angular app.

It depends on the `blocks.logger` module, because the implementation logs the exceptions.

##### blocks.router Module
The `blocks.router` module contains a routing helper module that assists in adding routes to the $routeProvider.

## Gulp Tasks

### Task Listing

- `gulp help`

    Displays all of the available gulp tasks.

### Code Analysis

- `gulp vet`

    Performs static code analysis on all javascript files. Runs jshint and jscs.

- `gulp vet --verbose`

    Displays all files affected and extended information about the code analysis.

- `gulp plato`

    Performs code analysis using plato on all javascript files. Plato generates a report in the reports folder.

### Testing

- `gulp serve-specs`

    Serves and browses to the spec runner html page and runs the unit tests in it. Injects any changes on the fly and re runs the tests. Quick and easy view of tests as an alternative to terminal via `gulp test`.

- `gulp unit-test`

    Runs all unit tests using karma runner, mocha, chai and sinon with phantomjs. Depends on vet task, for code analysis.

- `gulp unit-test --startServers`

    Runs all unit tests and midway tests. Cranks up a second node process to run a server for the midway tests to hit a web api.

- `gulp webdriver-update`
    
    Downloads and sets up Selenium Webdriver for e2e testing.
    
- `gulp e2e-test`
    
    Runs all e2e tests using Protractor with Selenium Webdriver.

- `gulp autotest`

    Runs a watch to run all unit tests.

- `gulp autotest --startServers`

    Runs a watch to run all unit tests and midway tests. Cranks up a second node process to run a server for the midway tests to hit a web api.

### Cleaning Up

- `gulp clean`

    Remove all files from the build and temp folders

- `gulp clean-images`

    Remove all images from the build folder

- `gulp clean-code`

    Remove all javascript and html from the build folder

- `gulp clean-fonts`

    Remove all fonts from the build folder

- `gulp clean-styles`

    Remove all styles from the build folder

### Fonts and Images

- `gulp fonts`

    Copy all fonts from source to the build folder

- `gulp images`

    Copy all images from source to the build folder

### Styles

- `gulp styles`

    Compile less files to CSS, add vendor prefixes, and copy to the build folder

### Bower Files

- `gulp wiredep`

    Looks up all bower components' main files and JavaScript source code, then adds them to the `index.html`.

    The `.bowerrc` file also runs this as a postinstall task whenever `bower install` is run.

### Angular HTML Templates

- `gulp templatecache`

    Create an Angular module that adds all HTML templates to Angular's $templateCache. This pre-fetches all HTML templates saving XHR calls for the HTML.

- `gulp templatecache --verbose`

    Displays all files affected by the task.

### Serving Development Code

- `gulp serve-dev`

    Serves the development code and launches it in a browser. The goal of building for development is to do it as fast as possible, to keep development moving efficiently. This task serves all code from the source folders and compiles less to css in a temp folder.

- `gulp serve-dev --nosync`

    Serves the development code without launching the browser.

- `gulp serve-dev --debug`

    Launch debugger with node-inspector.

- `gulp serve-dev --debug-brk`

    Launch debugger and break on 1st line with node-inspector.

### Building Production Code

- `gulp html`

    Optimize all javascript and styles, move to a build folder, and inject them into the new index.html

- `gulp build`

    Copies all fonts, copies images and runs `gulp html` to build the production code to the build folder.

### Serving Production Code

- `gulp serve-build`

    Serve the optimized code from the build folder and launch it in a browser.

- `gulp serve-build --nosync`

    Serve the optimized code from the build folder and manually launch the browser.

- `gulp serve-build --debug`

    Launch debugger with node-inspector.

- `gulp serve-build --debug-brk`

    Launch debugger and break on 1st line with node-inspector.

### Bumping Versions

- `gulp bump`

    Bump the minor version using semver.
    --type=patch // default
    --type=minor
    --type=major
    --type=pre
    --ver=1.2.3 // specific version

## License

MIT
