window.onload = function()
{
    var internalGetCallback = function(assertion) {
        if (assertion) {
            PersonaHelper.assertionReady(assertion);
        } else {
            PersonaHelper.assertionFailure();
        }
    };

    var origin = "__ORIGIN__";

    var internalSetPersistentCallback = function() {
        BrowserID.internal.get(origin, internalGetCallback, {silent: false})
    };


    BrowserID.internal.setPersistent(origin, internalSetPersistentCallback);
    console.log(BrowserID.internal);
    console.log("All done setting up persona!");
};