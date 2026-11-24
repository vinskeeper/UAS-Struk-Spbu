package controller;

import dao.ProdukDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.spbu.Bensin;
import model.spbu.Product;
import model.spbu.Solar;
import java.sql.SQLException;
import java.util.Locale;

public class ProdukController {
    @FXML private TextField nameField, priceField, ratingField;
    @FXML private ComboBox<String> typeBox;
    @FXML private Label ratingLabel, selectionLabel;
    @FXML private Button saveButton;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product,String> nameColumn,typeColumn,priceColumn,ratingColumn,statusColumn;
    private final ProdukDAO dao=new ProdukDAO();
    private static final Locale ID=Locale.forLanguageTag("id-ID");
    @FXML private void initialize(){
        typeBox.setItems(FXCollections.observableArrayList("BENSIN","SOLAR"));typeBox.getSelectionModel().selectFirst();
        typeBox.valueProperty().addListener((o,a,v)->{boolean b="BENSIN".equals(v);ratingLabel.setText(b?"Oktan":"Cetane");ratingField.setPromptText(b?"Nilai oktan":"Nilai cetane");});
        nameColumn.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getNama()));
        typeColumn.setCellValueFactory(c->new SimpleStringProperty(c.getValue() instanceof Bensin?"Bensin":"Solar"));
        priceColumn.setCellValueFactory(c->new SimpleStringProperty("Rp "+String.format(ID,"%,.0f",c.getValue().getHarga())));
        ratingColumn.setCellValueFactory(c->new SimpleStringProperty(c.getValue() instanceof Bensin b?String.valueOf(b.getOktan()):c.getValue() instanceof Solar s?String.valueOf(s.getCetane()):"-"));
        statusColumn.setCellValueFactory(c->new SimpleStringProperty(c.getValue().isAktif()?"Aktif":"Nonaktif"));
        productTable.getSelectionModel().selectedItemProperty().addListener((o,a,p)->select(p));refresh();
    }
    private void select(Product p){if(p==null)return;nameField.setText(p.getNama());priceField.setText(String.valueOf(p.getHarga()));
        if(p instanceof Bensin b){typeBox.setValue("BENSIN");ratingField.setText(String.valueOf(b.getOktan()));}
        else if(p instanceof Solar s){typeBox.setValue("SOLAR");ratingField.setText(String.valueOf(s.getCetane()));}
        selectionLabel.setText("Mengubah: "+p.getNama());saveButton.setText("Simpan Perubahan");}
    @FXML private void save(){
        double price;int rating;try{price=Double.parseDouble(priceField.getText().trim().replace(',','.'));rating=Integer.parseInt(ratingField.getText().trim());}
        catch(Exception e){warn("Isi harga dan nilai oktan/cetane dengan angka yang valid.");return;}
        Product selected=productTable.getSelectionModel().getSelectedItem();String name=nameField.getText()==null?"":nameField.getText().trim();
        if(name.isBlank()||price<0||rating<=0){warn("Isi nama, harga nonnegatif, dan nilai oktan/cetane positif.");return;}
        Product p="BENSIN".equals(typeBox.getValue())?new Bensin(selected==null?0:selected.getId(),name,price,rating,true):new Solar(selected==null?0:selected.getId(),name,price,rating,true);
        try{if(selected==null)dao.save(p);else dao.update(p);refresh();clear();}catch(SQLException e){error("Produk gagal disimpan",e.getMessage());}
    }
    @FXML private void clear(){productTable.getSelectionModel().clearSelection();nameField.clear();priceField.clear();ratingField.clear();typeBox.setValue("BENSIN");saveButton.setText("Simpan Produk");selectionLabel.setText("Produk baru.");}
    @FXML private void deactivate(){status(false);}
    @FXML private void activate(){status(true);}
    private void status(boolean active){Product p=productTable.getSelectionModel().getSelectedItem();if(p==null){warn("Pilih produk pada tabel terlebih dahulu.");return;}
        try{if(active)dao.activate(p.getId());else dao.deactivate(p.getId());refresh();}catch(SQLException e){error("Status produk gagal diubah",e.getMessage());}}
    @FXML private void delete(){Product p=productTable.getSelectionModel().getSelectedItem();if(p==null){warn("Pilih produk yang ingin dihapus.");return;}
        Alert a=new Alert(Alert.AlertType.CONFIRMATION,"Hapus produk "+p.getNama()+" secara permanen?");if(a.showAndWait().filter(ButtonType.OK::equals).isEmpty())return;
        try{dao.delete(p.getId());refresh();clear();}catch(SQLException e){error("Produk gagal dihapus",e.getMessage());}}
    private void refresh(){try{productTable.setItems(FXCollections.observableArrayList(dao.getProducts(false)));}catch(SQLException e){error("Produk tidak dapat dimuat",e.getMessage());}}
    private void warn(String s){new Alert(Alert.AlertType.WARNING,s).showAndWait();}
    private void error(String t,String s){Alert a=new Alert(Alert.AlertType.ERROR,s);a.setTitle(t);a.showAndWait();}
}
