package com.photon.photonchain.interfaces.utils;

/**
 * 分页对象
 *
 * @author lin
 */
public class PageObject {

    //当前页码
    private Integer pageNumber = 1;
    //每页记录数
    private Integer pageSize = 10;

    Integer sumRecord;

    //获取每页第一条记录的索引
    public Integer getFirstRecord() {
        return (pageNumber - 1) * pageSize;
    }


    //总页数
    public Integer getSumPage() {
        Integer sumPage = 1;
        if (sumRecord != null) {
            if (sumRecord <= pageSize) {
                sumPage = 1;
            } else {
                if (sumRecord % pageSize == 0) {
                    sumPage = sumRecord / pageSize;
                } else {
                    sumPage = sumRecord / pageSize + 1;
                }
            }
        }
        return sumPage;
    }


    public Integer getPageNumber() {
        if (this.pageNumber == null) {
            this.pageNumber = 1;
        }
        return this.pageNumber < 1 ? 1 : this.pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        if (this.pageSize == null) {
            this.pageSize = 10;
        }
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getSumRecord() {
        return sumRecord;
    }

    public PageObject setSumRecord(Integer sumRecord) {
        this.sumRecord = sumRecord;
        int sumPage = getSumPage();
        if (sumRecord % pageSize != 0 && this.getPageNumber() == sumPage) {
            this.pageSize = sumRecord % pageSize;
        }
        return this;
    }
}
