package org.ruaux.jdiscogs.data;

import org.ruaux.jdiscogs.data.xml.Master;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterRepository extends CrudRepository<Master, String> {

}
