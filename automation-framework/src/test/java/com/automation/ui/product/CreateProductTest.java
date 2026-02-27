package com.automation.ui.product;

import com.automation.annotations.FrameworkAnnotation;
import com.automation.base.BaseTest;
import com.automation.dataproviders.ProductDataProvider;
import com.automation.enums.CategoryType;
import com.automation.models.ProductType;
import com.automation.pages.admin.product.CreateProductPage;
import com.automation.pages.admin.product.DesignEditorPage;
import com.automation.pages.admin.product.ProductListPage;
import com.automation.utils.ProductDataManager;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("UI Tests")
@Feature("Product Management")
public class CreateProductTest extends BaseTest {

    private ProductListPage productListPage;

    @BeforeMethod(alwaysRun = true)
    public void loginAndNavigate() {
        loginWithSessionReuse();
        productListPage = new ProductListPage();
        productListPage.navigateToProductsPage(getBaseUrl());
    }

    @Test(description = "Create a POD product with random product base",
            dataProvider = "singleRandomProduct", dataProviderClass = ProductDataProvider.class)
    @FrameworkAnnotation(category = {CategoryType.REGRESSION},
            author = "Framework", description = "Create a POD product end-to-end")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Create POD Product")
    public void testCreatePODProduct(ProductType productType) {
        log.info("Creating POD product with base: {}", productType.getName());

        // Step 1: Click Create POD product → Design Editor
        DesignEditorPage designEditor = productListPage.clickCreatePODProduct();

        // Step 2: Search and select product type (retry with different product if not found)
        ProductType selectedProduct = productType;
        boolean found = designEditor.searchAndSelectProductType(selectedProduct.getName());
        while (!found) {
            log.warn("Product type '{}' not found, removing and trying another", selectedProduct.getName());
            ProductDataManager.getInstance().removeProduct(selectedProduct.getName());
            selectedProduct = ProductDataManager.getInstance().getRandomProduct();
            log.info("Trying product type: {}", selectedProduct.getName());
            found = designEditor.searchAndSelectProductType(selectedProduct.getName());
        }
        designEditor.clickSaveProductType();

        // Step 3: Add colors
        designEditor.clickAddColor();
        designEditor.selectMaxColors();
        designEditor.closeColorPanel();

        // Step 4: Upload design image
        designEditor.uploadRecommendedDesign();

        // Step 5: Save design → Create Product page
        CreateProductPage createProductPage = designEditor.saveDesign();

        // Step 6: Verify design saved
        createProductPage.verifyDesignSaved();

        // Step 7: Enter title and description
        createProductPage.enterTitleAndDescription(selectedProduct.getName());
        String title = createProductPage.getGeneratedTitle();

        // Step 8: Save product
        ProductListPage listPage = createProductPage.clickSave();
        listPage.waitForSaveSuccess();

        // Step 9: Search and verify product
        listPage.searchProduct(title);

        assertThat(listPage.isProductDisplayed(title))
                .as("Product '%s' should be displayed in product list", title)
                .isTrue();
    }
}
