# Simple shadow-grove app

## Development

Run `npm install` before you start development for the first time and each time you add a new JavaScript dependency.

If you're a beginner to Clojure and don't have a favorite setup yet, I recommend using [Visual Studio Code](https://code.visualstudio.com/) in combination with the [Calva extension](https://calva.io/).

Once you've installed Java, Node.js, NPM, Visual Studio Code, and Calva, you can open `src/core.clj`, [connect Calva to the project](https://calva.io/connect/), and start experimenting.

## Running tests

First, compile the JavaScript bundle containing all tests by executing `npx shadow-cljs compile test`.
Afterwards, run the tests by executing `node out/node-test.js`.


## Deploying

Execute `deploy.sh` to deploy with GitHub pages.
The resulting release build can be found in `../shadow-grove/docs`.
