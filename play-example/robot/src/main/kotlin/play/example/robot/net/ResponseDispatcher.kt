package play.example.robot.net

import play.mvc.Response

class ResponseDispatcher() {

  fun dispatch(response: Response, requester: Requester) {
    requester.getRequestParams<RequestParams>(response.header.sequenceNo)
  }
}
