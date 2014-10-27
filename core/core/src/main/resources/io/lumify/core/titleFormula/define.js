
function define(array, func) {
    for (var i = 0; i < array.length; i++) {
        println("// " + array[i]);
        var jsFile = TitleFormulaEvaluator.getJavaScriptFile(array[i]);
        println(jsFile);
    }
    return func(40, 2);
}
