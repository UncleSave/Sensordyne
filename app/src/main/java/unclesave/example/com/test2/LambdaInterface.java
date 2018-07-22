package unclesave.example.com.test2;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;
public interface LambdaInterface {

    /**
     * Invoke the Lambda function "AndroidBackendLambdaFunction".
     * The function name is the method name.
     * @param request
     */
    @LambdaFunction
    ResponseClass AndroidBackendLambdaFunction(RequestClass request);

}