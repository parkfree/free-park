package org.chenliang.freepark.model.rtmap;

import java.util.List;
import lombok.Data;

@Data
public class ProductsResponse {

  int code;
  InnerData data;
  int maCode;
  int status;
  String message;

  @Data
  public static class InnerData {
    List<Product> list;
    int page;
    int pageSize;
    int pages;
    int total;
  }

  @Data
  public static class Product {

    String activityCouponId;
    String activityId;
    int batchState;
    int collectionNum;
    int couponId;
    int exchangeAmount;
    String exchangeDesc;
    int exchangeLimit;
    int exchangeScheduleId;
    String exchangeScheduleName;
    String exchangeTimeDesc;
    int exchangeTransactions;
    String exchangeTransactionsDesc;
    String extendInfo;
    int getLimit;
    int getNum;
    int id;
    String industryName;
    int isAllShop;
    String logoUrl;
    String mainInfo;
    int memberType;
    int minPrice;
    int portalId;
    int pourChannel;
    int productType;
    int type;
  }
}
